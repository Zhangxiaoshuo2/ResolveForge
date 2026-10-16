package dev.resolveforge.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Component;

/** OpenAI-compatible HTTP embeddings. No random/hash fallback, no chat-model credential reuse. */
@Component
public class OpenAiEmbeddingProvider implements EmbeddingProvider {
    private final EmbeddingProperties config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    public OpenAiEmbeddingProvider(EmbeddingProperties config) { this.config = config; }
    public boolean configured() { return config.apiKey()!=null && !config.apiKey().isBlank(); }
    public String identity() { return config.baseUrl().replaceAll("/+$", "")+"|"+model()+"|"+dimensions(); }
    public String model() { return config.model(); }
    public int dimensions() { return config.dimensions(); }
    public List<double[]> embed(List<String> texts) {
        if (!configured()) throw new IllegalStateException("EMBEDDING_NOT_CONFIGURED");
        if (texts.isEmpty() || texts.size()>10) throw new IllegalArgumentException("embedding_batch_requires_1_to_10_texts");
        try {
            var body = json.writeValueAsString(Map.of("model", model(), "input", texts, "dimensions", dimensions(), "encoding_format", "float"));
            var request = HttpRequest.newBuilder(URI.create(config.baseUrl().replaceAll("/+$", "")+"/embeddings"))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds())).header("Authorization", "Bearer "+config.apiKey())
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode()!=200) throw new IllegalStateException("EMBEDDING_HTTP_"+response.statusCode());
            var root = json.readTree(response.body());
            if (root.hasNonNull("model") && !model().equals(root.path("model").asText())) throw new IllegalStateException("EMBEDDING_MODEL_MISMATCH");
            var data = root.path("data");
            if (!data.isArray() || data.size()!=texts.size()) throw new IllegalStateException("EMBEDDING_COUNT_MISMATCH");
            double[][] ordered = new double[texts.size()][];
            for (var item : data) {
                if (!item.path("index").isIntegralNumber()) throw new IllegalStateException("EMBEDDING_INDEX_INVALID");
                int index = item.path("index").asInt();
                if (index<0 || index>=ordered.length || ordered[index]!=null) throw new IllegalStateException("EMBEDDING_INDEX_INVALID");
                var values = item.path("embedding");
                if (!values.isArray() || values.size()!=dimensions()) throw new IllegalStateException("EMBEDDING_DIMENSIONS_MISMATCH");
                double[] vector = new double[dimensions()];
                for (int i=0;i<vector.length;i++) {
                    if (!values.get(i).isNumber()) throw new IllegalStateException("EMBEDDING_NON_NUMERIC");
                    vector[i] = values.get(i).asDouble();
                }
                Vectors.validate(vector, dimensions()); ordered[index] = vector;
            }
            return List.of(ordered);
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException("EMBEDDING_INTERRUPTED"); }
        catch (IllegalStateException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalStateException("EMBEDDING_TRANSPORT_OR_JSON_FAILED"); }
    }
}
