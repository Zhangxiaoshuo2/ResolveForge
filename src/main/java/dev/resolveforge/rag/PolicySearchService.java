package dev.resolveforge.rag;

import dev.resolveforge.api.ApiException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PolicySearchService {
    public enum Mode {AUTO,BM25,VECTOR,HYBRID}
    public record Evidence(String sourceId,String chunkId,String title,String content,double score,String version,String effectiveDate) {}
    public record Result(String code,String mode,String fallbackReason,List<Evidence> evidence) {}
    private static final Logger log=LoggerFactory.getLogger(PolicySearchService.class);
    private final PolicyIndex index;
    private final EmbeddingProvider provider;
    private final RagProperties config;
    // Query reuse for evaluation and repeated questions; bounded, in-process, not persisted private text.
    private final Map<String,double[]> queries=Collections.synchronizedMap(new LinkedHashMap<>(256,0.75f,true) {
        protected boolean removeEldestEntry(Map.Entry<String,double[]> entry) {return size()>256;}
    });
    public PolicySearchService(PolicyIndex index,EmbeddingProvider provider,RagProperties config) {this.index=index;this.provider=provider;this.config=config;}
    public Result search(String query,Integer topK,Mode requested) {
        if(query==null || query.isBlank() || query.length()>1000 || (topK!=null && (topK<1 || topK>20)))
            throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_SEARCH","查询须为 1 至 1000 字符，topK 为 1 至 20");
        long start=System.nanoTime();
        Mode mode=requested==null?Mode.AUTO:requested;
        if(mode==Mode.AUTO) mode=Mode.HYBRID;
        String actual=mode==Mode.BM25?"BM25_ONLY":mode.name(),fallback=null;
        List<Bm25Index.Hit> lexical=List.of(),semantic=List.of();
        if(mode!=Mode.VECTOR) lexical=index.bm25(query,config.recallK(),config.bm25MinScore(),config.lexicalMinCoverage());
        if(mode!=Mode.BM25) {
            if(!index.status().vectorAvailable()) {
                if(mode==Mode.VECTOR) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"VECTOR_UNAVAILABLE","向量索引未就绪，不能执行 Vector 评测");
                actual="BM25_ONLY";fallback=index.status().fallbackReason();
            } else {
                try {
                    String key=KnowledgeLoader.hash(provider.identity()+"|"+query);
                    double[] embedding=queries.get(key);
                    if(embedding==null) {
                        var batch=provider.embed(List.of(query));
                        if(batch.size()!=1) throw new IllegalStateException("EMBEDDING_COUNT_MISMATCH");
                        embedding=batch.getFirst();Vectors.validate(embedding,provider.dimensions());queries.put(key,embedding);
                    }
                    semantic=index.vector(embedding,config.recallK(),config.vectorMinScore());
                } catch(RuntimeException ex) {
                    log.warn("policy_query_embedding_failed type={}",ex.getClass().getSimpleName());
                    if(mode==Mode.VECTOR) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"VECTOR_UNAVAILABLE","查询向量生成失败，未执行 Vector 评测");
                    actual="BM25_ONLY";fallback="EMBEDDING_QUERY_FAILED";
                }
            }
        }
        List<Bm25Index.Hit> ranked=actual.equals("HYBRID")?RrfFusion.fuse(lexical,semantic,config.rrfK()).stream()
                .filter(h->h.score()>=config.fusionMinScore()).toList():actual.equals("VECTOR")?semantic:lexical;
        List<Evidence> evidence=ranked.stream().limit(topK==null?config.topK():topK).map(h->new Evidence(h.chunk().sourceId(),h.chunk().chunkId(),h.chunk().title(),
                h.chunk().content(),h.score(),h.chunk().version(),h.chunk().effectiveDate())).toList();
        String code=evidence.isEmpty()?"NO_RELEVANT_EVIDENCE":"OK";
        log.info("policy_search mode={} sources={} durationMs={} outcome={} fallback={}",actual,evidence.stream().map(Evidence::chunkId).toList(),(System.nanoTime()-start)/1_000_000,code,fallback);
        return new Result(code,actual,fallback,evidence);
    }
}
