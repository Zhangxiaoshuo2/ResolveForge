package dev.resolveforge.rag;

import java.util.*;

public final class RrfFusion {
    private RrfFusion() {}
    public static List<Bm25Index.Hit> fuse(List<Bm25Index.Hit> lexical,List<Bm25Index.Hit> semantic,int k) {
        Map<String,Double> scores=new HashMap<>(); Map<String,PolicyChunk> chunks=new HashMap<>();
        for(var list:List.of(lexical,semantic)) {
            Set<String> seen=new HashSet<>();int rank=0;
            for(var hit:list) {
                if(!seen.add(hit.chunk().chunkId())) continue;
                rank++;chunks.put(hit.chunk().chunkId(),hit.chunk());
                scores.merge(hit.chunk().chunkId(),1.0/(k+rank),Double::sum);
            }
        }
        return scores.entrySet().stream().map(e->new Bm25Index.Hit(chunks.get(e.getKey()),e.getValue()))
                .sorted(Comparator.comparingDouble(Bm25Index.Hit::score).reversed().thenComparing(h->h.chunk().chunkId())).toList();
    }
}
