package dev.resolveforge;

import dev.resolveforge.commerce.CommerceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

/** Explicit opt-in: mvn -Ppostgres-it verify. Requires Docker, never silently counted as passed. */
@Testcontainers
@SpringBootTest
class PostgresIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("app.auth.alice-token", () -> "alice-test-token-0000000000000000");
        r.add("app.auth.bob-token", () -> "bob-test-token-000000000000000000");
        r.add("app.auth.service-token", () -> "service-test-token-00000000000000");
        r.add("app.model.api-key", () -> "");
    }
    @Autowired CommerceRepository commerce;
    @Test void postgresMigrationsAndOwnership() {
        assertThat(commerce.order("alice", "RF1001").status()).isEqualTo("PAID");
        assertThat(commerce.logistics("alice", "RF1003").status()).isEqualTo("IN_TRANSIT");
        assertThatThrownBy(() -> commerce.order("alice", "RF2001")).hasMessageContaining("订单不存在或不属于当前用户");
    }
}
