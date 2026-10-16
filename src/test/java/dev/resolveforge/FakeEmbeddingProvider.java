package dev.resolveforge;

import dev.resolveforge.rag.EmbeddingProvider;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Explicit hand-authored basis vectors for mechanics tests, never semantic quality metrics. */
final class FakeEmbeddingProvider implements EmbeddingProvider {
    final AtomicInteger embeddedTexts=new AtomicInteger();
    boolean enabled=true;
    boolean fail=false;
    private final String model;
    private final int dimensions;
    FakeEmbeddingProvider() {this("fake-basis-v1",4);}
    FakeEmbeddingProvider(String model,int dimensions) {this.model=model;this.dimensions=dimensions;}
    public boolean configured() {return enabled;}
    public String identity() {return "fake://explicit-basis/"+model+"/"+dimensions;}
    public String model() {return model;}
    public int dimensions() {return dimensions;}
    public List<double[]> embed(List<String> texts) {
        if(fail) throw new IllegalStateException("FAKE_EMBEDDING_FAILURE");
        embeddedTexts.addAndGet(texts.size());
        return texts.stream().map(text->{
            double[] vector=new double[dimensions];
            String topic=text.contains("\n")?text.substring(0,text.indexOf('\n')):text;
            int slot=topic.contains("ZXYUNRELATEDTOPIC")||topic.contains("会员积分")?3:topic.contains("退款")||topic.contains("未发货")?0:topic.contains("物流")||topic.contains("运输")||topic.contains("丢件")?1:2;
            vector[Math.min(slot,dimensions-1)]=1;return vector;
        }).toList();
    }
}
