package com.github.dimitryivaniuta.experimentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end integration test using PostgreSQL and Redis Testcontainers.
 */
@Testcontainers
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExperimentationApplicationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("ab_testing")
            .withUsername("ab_user")
            .withPassword("ab_password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379);

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Supplies container connection details to Spring Boot.
     *
     * @param registry dynamic property registry
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ':' + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:65535");
        registry.add("experimentation.outbox.enabled", () -> "false");
        registry.add("experimentation.privacy.hmac-secret", () -> "integration-secret-at-least-32-characters");
    }

    /**
     * Verifies stable assignment and per-variant metric aggregation through real HTTP and PostgreSQL.
     */
    @Test
    void shouldKeepStableAssignmentAndAggregateMetrics() {
        Map<?, ?> first = getAssignment();
        Map<?, ?> second = getAssignment();

        assertThat(second.get("variantKey")).isEqualTo(first.get("variantKey"));

        webTestClient.post()
                .uri("/api/v1/events")
                .bodyValue(Map.of(
                        "experimentKey", "checkout_button_color",
                        "userId", "integration-user-1",
                        "eventName", "purchase",
                        "value", 19.99,
                        "metadata", Map.of("currency", "EUR")
                ))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.accepted").isEqualTo(true)
                .jsonPath("$.variantKey").isEqualTo(first.get("variantKey"));

        webTestClient.get()
                .uri("/api/v1/metrics/checkout_button_color")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.metrics[?(@.metricName=='purchase')]").exists();

        webTestClient.get()
                .uri("/api/v1/metrics/checkout_button_color/summary")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.variants[0].exposureCount").exists();
    }

    private Map<?, ?> getAssignment() {
        return webTestClient.get()
                .uri("/api/v1/flags/checkout_button_color/assignment")
                .header("X-User-Id", "integration-user-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
    }
}
