package dev.resolveforge.rag;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;

@Component
@DependsOnDatabaseInitialization
public class PolicyIndex {
    public record Status(int chunks, boolean embeddingConfigured, boolean vectorAvailable, String model, int dimensions,
                         int cacheHits, int embeddedChunks, String corpusHash, String fallbackReason) {}
    private static final Logger log=LoggerFactory.getLogger(PolicyIndex.class);
    private final KnowledgeLoader loader;
    private final EmbeddingProvider provider;
    private final EmbeddingCache cache;
    private List<PolicyChunk> chunks=List.of();
    private Map<String,double[]> vectors=Map.of();
    private Bm25Index bm25;
    private Status status;
    public PolicyIndex(KnowledgeLoader loader, EmbeddingProvider provider, EmbeddingCache cache) {this.loader=loader;this.provider=provider;this.cache=cache;}
    @PostConstruct
    public void initialize() {
        long start=System.nanoTime();
        chunks=loader.load(); bm25=new Bm25Index(chunks);
        int hits=0,generated=0;
        String reason=provider.configured()?null:"EMBEDDING_NOT_CONFIGURED";
        Map<String,double[]> collected=new HashMap<>();
        if(provider.configured()) {
            try {
                List<PolicyChunk> missing=new ArrayList<>();
                for(var chunk:chunks) {
                    var stored=cache.get(provider,chunk);
                    if(stored.isPresent()) {collected.put(chunk.chunkId(),stored.get());hits++;}
                    else missing.add(chunk);
                }
                for(int from=0;from<missing.size();from+=10) {
                    var batch=missing.subList(from,Math.min(from+10,missing.size()));
                    var result=provider.embed(batch.stream().map(PolicyChunk::embeddingText).toList());
                    if(result.size()!=batch.size()) throw new IllegalStateException("EMBEDDING_COUNT_MISMATCH");
                    for(int i=0;i<batch.size();i++) {
                        Vectors.validate(result.get(i),provider.dimensions());
                        cache.put(provider,batch.get(i),result.get(i)); collected.put(batch.get(i).chunkId(),result.get(i)); generated++;
                    }
                }
            } catch(RuntimeException ex) {
                reason="EMBEDDING_INDEX_FAILED";collected.clear();
                log.warn("policy_index_vector_unavailable code={} type={}", reason,ex.getClass().getSimpleName());
            }
        }
        vectors=Map.copyOf(collected);
        status=new Status(chunks.size(),provider.configured(),vectors.size()==chunks.size(),provider.model(),provider.dimensions(),hits,generated,
                KnowledgeLoader.hash(chunks.stream().map(c->c.chunkId()+":"+c.documentHash()).reduce("",(a,b)->a+"\n"+b)),reason);
        log.info("policy_index_ready chunks={} mode={} cacheHits={} embeddedChunks={} model={} dimensions={} durationMs={}",
                chunks.size(),status.vectorAvailable()?"HYBRID":"BM25_ONLY",hits,generated,provider.model(),provider.dimensions(),(System.nanoTime()-start)/1_000_000);
    }
    public Status status() {return status;}
    public List<PolicyChunk> chunks() {return chunks;}
    public List<Bm25Index.Hit> bm25(String query,int k,double threshold,double coverage) {return bm25.search(query,k,threshold,coverage);}
    public List<Bm25Index.Hit> vector(double[] query,int k,double threshold) {
        Vectors.validate(query,provider.dimensions());
        return chunks.stream().map(c->new Bm25Index.Hit(c,Vectors.cosine(query,vectors.get(c.chunkId()))))
                .filter(hit->hit.score()>=threshold)
                .sorted(Comparator.comparingDouble(Bm25Index.Hit::score).reversed().thenComparing(h->h.chunk().chunkId())).limit(k).toList();
    }
    @PreDestroy public void close() {if(bm25!=null) bm25.close();}
}
