package dev.resolveforge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmbeddingCache {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();
    public EmbeddingCache(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private String key(EmbeddingProvider p, PolicyChunk c) { return KnowledgeLoader.hash(p.identity()+"|"+c.chunkId()+"|"+c.contentHash()); }
    public Optional<double[]> get(EmbeddingProvider p, PolicyChunk c) {
        var rows = jdbc.query("SELECT vector_json FROM policy_embedding_cache WHERE cache_key=? AND model_name=? AND dimensions=? AND content_hash=? AND provider_identity=?",
                (rs,n)->rs.getString(1), key(p,c), p.model(), p.dimensions(), c.contentHash(), p.identity());
        if(rows.isEmpty()) return Optional.empty();
        try {
            double[] vector=json.readValue(rows.getFirst(), double[].class); Vectors.validate(vector,p.dimensions());
            // Metadata can change without changing this chunk's embedding input.
            jdbc.update("UPDATE policy_embedding_cache SET document_hash=? WHERE cache_key=?", c.documentHash(),key(p,c));
            return Optional.of(vector);
        } catch(Exception ex) { return Optional.empty(); }
    }
    public void put(EmbeddingProvider p, PolicyChunk c, double[] vector) {
        Vectors.validate(vector,p.dimensions());
        try {
            String encoded=json.writeValueAsString(vector);
            int updated=jdbc.update("UPDATE policy_embedding_cache SET vector_json=?,document_hash=? WHERE cache_key=?", encoded,c.documentHash(),key(p,c));
            if(updated==0) jdbc.update("INSERT INTO policy_embedding_cache(cache_key,provider_identity,model_name,dimensions,chunk_id,document_hash,content_hash,vector_json) VALUES (?,?,?,?,?,?,?,?)",
                    key(p,c),p.identity(),p.model(),p.dimensions(),c.chunkId(),c.documentHash(),c.contentHash(),encoded);
        } catch(com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("embedding_cache_encode_failed"); }
    }
}
