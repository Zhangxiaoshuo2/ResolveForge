package dev.resolveforge.rag;

public final class Vectors {
    private Vectors() {}
    public static void validate(double[] vector, int dimensions) {
        if (vector==null || vector.length!=dimensions) throw new IllegalStateException("EMBEDDING_DIMENSIONS_MISMATCH");
        double norm=0;
        for(double v : vector) { if(!Double.isFinite(v)) throw new IllegalStateException("EMBEDDING_NON_FINITE"); norm+=v*v; }
        if (!Double.isFinite(norm) || norm<=0) throw new IllegalStateException("EMBEDDING_ZERO_OR_INVALID_NORM");
    }
    public static double cosine(double[] a, double[] b) {
        if(a.length!=b.length) throw new IllegalArgumentException("vector_dimensions_differ");
        double dot=0, na=0, nb=0;
        for(int i=0;i<a.length;i++) { dot+=a[i]*b[i]; na+=a[i]*a[i]; nb+=b[i]*b[i]; }
        return Math.max(-1, Math.min(1, dot / Math.sqrt(na*nb)));
    }
}
