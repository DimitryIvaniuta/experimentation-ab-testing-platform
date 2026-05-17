package com.github.dimitryivaniuta.experimentation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.experimentation.config.ExperimentationProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for privacy-safe HMAC user hashing.
 */
class UserHasherTest {

    private final UserHasher userHasher = new UserHasher(new ExperimentationProperties(
            "admin-token",
            new ExperimentationProperties.PrivacyProperties("test-secret-at-least-32-characters"),
            new ExperimentationProperties.AssignmentCacheProperties(Duration.ofDays(30)),
            new ExperimentationProperties.KafkaProperties("exposure", "tracking"),
            new ExperimentationProperties.OutboxProperties(false, 1000, 50, 10)
    ));

    /**
     * Verifies that hashes are deterministic and do not expose raw user identifiers.
     */
    @Test
    void shouldHashUserIdDeterministically() {
        String first = userHasher.hash("user-123");
        String second = userHasher.hash("user-123");

        assertThat(second).isEqualTo(first);
        assertThat(first).hasSize(64);
        assertThat(first).doesNotContain("user-123");
    }

    /**
     * Verifies that different user ids produce different hashes.
     */
    @Test
    void shouldProduceDifferentHashesForDifferentUsers() {
        assertThat(userHasher.hash("user-123")).isNotEqualTo(userHasher.hash("user-456"));
    }
}
