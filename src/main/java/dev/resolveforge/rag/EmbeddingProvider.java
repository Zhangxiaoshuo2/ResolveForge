package dev.resolveforge.rag;

import java.util.List;

public interface EmbeddingProvider {
    boolean configured();
    String identity();
    String model();
    int dimensions();
    List<double[]> embed(List<String> texts);
}
