package dev.resolveforge.rag;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.embedding")
public record EmbeddingProperties(String apiKey, @NotBlank String baseUrl, @NotBlank String model,
        @Min(1) @Max(4096) int dimensions, @Min(1) @Max(120) int timeoutSeconds) {}
