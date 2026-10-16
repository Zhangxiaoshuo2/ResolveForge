package dev.resolveforge.rag;

public record PolicyChunk(String sourceId, String chunkId, String title, String content,
        String version, String effectiveDate, String documentHash, String contentHash) {
    public String embeddingText() { return title + "\n" + content; }
}
