package com.github.dimitryivaniuta.experimentation.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe application configuration for privacy, Kafka, admin protection, cache, and outbox behavior.
 *
 * @param adminToken admin token required by write/admin endpoints
 * @param privacy privacy-related configuration
 * @param assignmentCache Redis assignment cache configuration
 * @param kafka Kafka topic configuration
 * @param outbox transactional outbox publisher configuration
 */
@Validated
@ConfigurationProperties(prefix = "experimentation")
public record ExperimentationProperties(
        @NotBlank @Size(min = 12) String adminToken,
        @Valid @NotNull PrivacyProperties privacy,
        @Valid @NotNull AssignmentCacheProperties assignmentCache,
        @Valid @NotNull KafkaProperties kafka,
        @Valid @NotNull OutboxProperties outbox
) {

    /**
     * Privacy configuration used to avoid storing or publishing raw user identifiers.
     *
     * @param hmacSecret secret used by HMAC-SHA256 user hashing
     */
    public record PrivacyProperties(@NotBlank @Size(min = 32) String hmacSecret) {
    }

    /**
     * Redis cache configuration for stable assignments.
     *
     * @param ttl time-to-live for cached assignments
     */
    public record AssignmentCacheProperties(@NotNull Duration ttl) {
    }

    /**
     * Kafka topic configuration.
     *
     * @param exposureTopic topic for variant exposure events
     * @param trackingTopic topic for custom tracking/conversion events
     */
    public record KafkaProperties(@NotBlank String exposureTopic, @NotBlank String trackingTopic) {
    }

    /**
     * Outbox configuration used by the scheduled Kafka publisher.
     *
     * @param enabled enables the scheduled publisher
     * @param pollDelayMs delay between outbox polling attempts
     * @param batchSize maximum number of events sent per poll
     * @param maxAttempts maximum retry attempts before events remain failed
     */
    public record OutboxProperties(
            boolean enabled,
            @Min(100) long pollDelayMs,
            @Min(1) @Max(500) int batchSize,
            @Min(1) @Max(100) int maxAttempts
    ) {
    }
}
