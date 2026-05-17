package com.github.dimitryivaniuta.experimentation.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox row waiting for Kafka publication.
 *
 * @param id outbox event identifier
 * @param aggregateType aggregate type name
 * @param aggregateKey aggregate routing key
 * @param eventType semantic event type
 * @param kafkaTopic target Kafka topic
 * @param kafkaKey target Kafka key
 * @param payloadJson JSON payload
 * @param status application-enforced status value
 * @param attempts publication attempts
 * @param createdAt creation timestamp
 */
public record OutboxRecord(
        UUID id,
        String aggregateType,
        String aggregateKey,
        String eventType,
        String kafkaTopic,
        String kafkaKey,
        String payloadJson,
        String status,
        int attempts,
        Instant createdAt
) {
}
