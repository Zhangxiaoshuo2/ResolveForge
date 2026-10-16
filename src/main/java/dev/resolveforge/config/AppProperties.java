package dev.resolveforge.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app")
public record AppProperties(@Valid Auth auth, @Valid Commerce commerce, @Valid Model model) {
    public record Auth(@NotBlank String aliceToken, @NotBlank String bobToken,
                       @NotBlank String serviceToken) {
        @AssertTrue(message = "Alice, Bob and service tokens must be distinct and at least 24 characters")
        public boolean isSecure() {
            return aliceToken != null && bobToken != null && serviceToken != null
                    && aliceToken.length() >= 24 && bobToken.length() >= 24 && serviceToken.length() >= 24
                    && !aliceToken.equals(bobToken) && !aliceToken.equals(serviceToken) && !bobToken.equals(serviceToken);
        }
    }
    public record Commerce(@NotBlank String baseUrl) {}
    public record Model(String apiKey, @NotBlank String baseUrl, @NotBlank String endpointPath,
                        @NotBlank String name, @Min(5) @Max(300) int timeoutSeconds) {}
}
