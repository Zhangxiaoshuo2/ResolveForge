package dev.resolveforge.rag;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.rag")
public record RagProperties(@Min(1) @Max(20) int topK, @Min(5) @Max(100) int recallK,
        @DecimalMin("0.0") double bm25MinScore, @DecimalMin("-1.0") @DecimalMax("1.0") double vectorMinScore,
        @Min(1) int rrfK, @DecimalMin("0.0") double fusionMinScore,
        @DecimalMin("0.0") @DecimalMax("1.0") double lexicalMinCoverage) {}
