package dev.resolveforge;

import dev.resolveforge.rag.*;
import io.agentscope.core.agent.RuntimeContext;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;

class RagMechanicsTest {
    private final KnowledgeLoader loader=new KnowledgeLoader();
    private final List<PolicyIndex> opened=new ArrayList<>();
    private EmbeddingCache cache;
    private JdbcTemplate jdbc;
    @BeforeEach void setup() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:rag_"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","");
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();jdbc=new JdbcTemplate(ds);cache=new EmbeddingCache(jdbc);
    }
    @AfterEach void close() {opened.forEach(PolicyIndex::close);}
    private PolicyIndex index(KnowledgeLoader docs,FakeEmbeddingProvider p) {
        var index=new PolicyIndex(docs,p,cache);index.initialize();opened.add(index);return index;
    }
    private PolicySearchService service(PolicyIndex i,FakeEmbeddingProvider p) {
        return new PolicySearchService(i,p,new RagProperties(5,12,1.2,0.6,60,0,0.35));
    }
    @Test void markdownMetadataAndStableChunkIdentity() {
        var chunks=loader.load();assertThat(chunks).hasSize(19);
        assertThat(chunks).extracting(PolicyChunk::chunkId).doesNotHaveDuplicates();
        assertThat(chunks).allSatisfy(c->{assertThat(c.documentHash()).hasSize(64);assertThat(c.contentHash()).hasSize(64);assertThat(c.version()).isEqualTo("demo-2026-10-v1");});
        String doc="---\nsourceId: DEMO-TEST\nversion: v1\neffectiveDate: 2026-10-01\nkind: DEMO\n---\n# 演示\n## 标题 {#clause}\n段落一。\n\n段落二。";
        var first=loader.parse(doc).getFirst();var next=loader.parse(doc.replace("version: v1","version: v2")).getFirst();
        assertThat(first.chunkId()).isEqualTo("DEMO-TEST#clause-p1");assertThat(next.chunkId()).isEqualTo(first.chunkId());
        assertThat(first.content()).contains("段落一。\n\n段落二。");assertThat(next.documentHash()).isNotEqualTo(first.documentHash());
        assertThat(next.contentHash()).isEqualTo(first.contentHash());
        assertThatThrownBy(()->loader.parse(doc.replace("kind: DEMO","kind: REAL"))).hasMessage("knowledge_must_be_demo");
    }
    @Test void warmCacheAndModelDimensionChangesCannotReuseWrongVectors() {
        var p=new FakeEmbeddingProvider();var first=index(loader,p);
        assertThat(first.status().embeddedChunks()).isEqualTo(19);
        var second=index(loader,p);assertThat(second.status().cacheHits()).isEqualTo(19);assertThat(second.status().embeddedChunks()).isZero();
        assertThat(p.embeddedTexts.get()).isEqualTo(19);
        var modelChanged=new FakeEmbeddingProvider("fake-basis-v2",4);assertThat(index(loader,modelChanged).status().cacheHits()).isZero();
        assertThat(modelChanged.embeddedTexts.get()).isEqualTo(19);
        var dimensionChanged=new FakeEmbeddingProvider("fake-basis-v1",5);assertThat(index(loader,dimensionChanged).status().cacheHits()).isZero();
        assertThat(dimensionChanged.embeddedTexts.get()).isEqualTo(19);
    }
    @Test void contentChangeEmbedsOnlyChangedChunkAndCorruptCacheIsNotUsed() {
        var p=new FakeEmbeddingProvider();index(loader,p);
        var original=loader.load();var first=original.getFirst();
        var changed=new ArrayList<>(original);changed.set(0,new PolicyChunk(first.sourceId(),first.chunkId(),first.title(),first.content()+"\n新增测试段落",first.version(),first.effectiveDate(),KnowledgeLoader.hash("changed-document"),KnowledgeLoader.hash("changed-input")));
        KnowledgeLoader updated=new KnowledgeLoader(){public List<PolicyChunk> load(){return changed;}};
        var result=index(updated,p);assertThat(result.status().embeddedChunks()).isEqualTo(1);assertThat(result.status().cacheHits()).isEqualTo(18);
        jdbc.update("UPDATE policy_embedding_cache SET vector_json='[0,0,0,0]'");
        var repaired=index(updated,p);assertThat(repaired.status().embeddedChunks()).isEqualTo(19);assertThat(repaired.status().vectorAvailable()).isTrue();
    }
    @Test void bm25ChineseAndRrfUseRankRatherThanRawScore() {
        try(var index=new Bm25Index(loader.load())) {
            assertThat(index.tokens("未发货订单可以退款吗")).isNotEmpty();
            assertThat(index.search("未发货订单退款",5,0,0)).extracting(h->h.chunk().chunkId()).contains("DEMO-REFUND#unshipped-p1");
            assertThat(index.search("ZXYUNRELATEDTOPIC",5,0,0)).isEmpty();
        }
        var c=loader.load();var a=new Bm25Index.Hit(c.get(0),999);var b=new Bm25Index.Hit(c.get(1),0.01);var d=new Bm25Index.Hit(c.get(2),0.99);
        var fused=RrfFusion.fuse(List.of(a,b,b),List.of(b,d),60);
        assertThat(fused.getFirst().chunk()).isEqualTo(b.chunk());assertThat(fused.getFirst().score()).isCloseTo(1.0/61+1.0/62,within(1e-12));
        assertThat(fused).hasSize(3);
    }
    @Test void missingConfigurationAndQueryFailureReportActualFallbackMode() {
        var disabled=new FakeEmbeddingProvider();disabled.enabled=false;var cold=index(loader,disabled);var only=service(cold,disabled);
        var result=only.search("未发货订单退款",5,PolicySearchService.Mode.AUTO);
        assertThat(result.mode()).isEqualTo("BM25_ONLY");assertThat(result.fallbackReason()).isEqualTo("EMBEDDING_NOT_CONFIGURED");
        assertThat(disabled.embeddedTexts.get()).isZero();
        assertThatThrownBy(()->only.search("未发货退款",5,PolicySearchService.Mode.VECTOR)).hasMessageContaining("向量索引未就绪");
        var p=new FakeEmbeddingProvider();var active=index(loader,p);var search=service(active,p);p.fail=true;
        var fallback=search.search("未发货订单退款",5,PolicySearchService.Mode.HYBRID);
        assertThat(fallback.mode()).isEqualTo("BM25_ONLY");assertThat(fallback.fallbackReason()).isEqualTo("EMBEDDING_QUERY_FAILED");
    }
    @Test void failedIndexNeverReportsHybridAndNoEvidenceIsExplicit() {
        var p=new FakeEmbeddingProvider();p.fail=true;var failed=index(loader,p);
        assertThat(failed.status().vectorAvailable()).isFalse();assertThat(failed.status().fallbackReason()).isEqualTo("EMBEDDING_INDEX_FAILED");
        p.fail=false;var ready=index(loader,p);var search=service(ready,p);
        var result=search.search("ZXYUNRELATEDTOPIC",5,PolicySearchService.Mode.HYBRID);
        assertThat(result.code()).isEqualTo("NO_RELEVANT_EVIDENCE");assertThat(result.evidence()).isEmpty();assertThat(result.mode()).isEqualTo("HYBRID");
        assertThat(new PolicyTools(search).searchPolicy("未发货退款",RuntimeContext.empty()).code()).isEqualTo("POLICY_UNAVAILABLE");
    }
    @Test void cosineAndQueryCacheAreDeterministic() {
        assertThat(Vectors.cosine(new double[]{1,0},new double[]{0,1})).isZero();
        assertThat(Vectors.cosine(new double[]{2,0},new double[]{1,0})).isEqualTo(1);
        assertThatThrownBy(()->Vectors.validate(new double[]{0,0},2)).hasMessageContaining("ZERO");
        assertThatThrownBy(()->Vectors.validate(new double[]{Double.NaN,1},2)).hasMessageContaining("NON_FINITE");
        var p=new FakeEmbeddingProvider();var search=service(index(loader,p),p);int before=p.embeddedTexts.get();
        search.search("未发货退款",5,PolicySearchService.Mode.VECTOR);search.search("未发货退款",5,PolicySearchService.Mode.HYBRID);
        assertThat(p.embeddedTexts.get()).isEqualTo(before+1);
    }
}
