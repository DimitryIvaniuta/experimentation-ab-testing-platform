package com.github.dimitryivaniuta.experimentation.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.experimentation.domain.OutboxRecord;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for the transactional outbox table.
 */
@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    /** Application-level status for events waiting for publishing. */
    public static final String STATUS_PENDING = "PENDING";

    /** Application-level status for events claimed by one publisher worker. */
    public static final String STATUS_PROCESSING = "PROCESSING";

    /** Application-level status for events successfully published to Kafka. */
    public static final String STATUS_SENT = "SENT";

    /** Application-level status for events that failed and can be retried. */
    public static final String STATUS_FAILED = "FAILED";

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    /**
     * Saves a pending outbox event.
     *
     * @param aggregateType aggregate type name
     * @param aggregateKey aggregate key
     * @param eventType semantic event type
     * @param kafkaTopic target Kafka topic
     * @param kafkaKey target Kafka key
     * @param payload payload serialized as JSON
     * @return saved outbox event id
     */
    public Mono<UUID> savePending(final String aggregateType, final String aggregateKey, final String eventType,
                                  final String kafkaTopic, final String kafkaKey, final Map<String, Object> payload) {
        UUID id = UUID.randomUUID();
        return databaseClient.sql("""
                        INSERT INTO outbox_events
                            (id, aggregate_type, aggregate_key, event_type, kafka_topic, kafka_key, payload_json, status, attempts, created_at)
                        VALUES
                            (:id, :aggregateType, :aggregateKey, :eventType, :kafkaTopic, :kafkaKey, CAST(:payloadJson AS jsonb), :status, 0, :createdAt)
                        """)
                .bind("id", id)
                .bind("aggregateType", aggregateType)
                .bind("aggregateKey", aggregateKey)
                .bind("eventType", eventType)
                .bind("kafkaTopic", kafkaTopic)
                .bind("kafkaKey", kafkaKey)
                .bind("payloadJson", toJson(payload))
                .bind("status", STATUS_PENDING)
                .bind("createdAt", Instant.now())
                .fetch()
                .rowsUpdated()
                .thenReturn(id);
    }

    /**
     * Atomically claims pending or retryable failed events for one publisher worker.
     *
     * <p>The {@code FOR UPDATE SKIP LOCKED} pattern prevents duplicate publishing when several application instances
     * run the outbox scheduler at the same time. Attempts are incremented on claim, not on final failure, so each
     * Kafka send attempt is counted exactly once.</p>
     *
     * @param limit maximum number of events to claim
     * @param maxAttempts maximum attempts allowed
     * @return claimed outbox events
     */
    public Flux<OutboxRecord> claimPublishable(final int limit, final int maxAttempts) {
        return databaseClient.sql("""
                        WITH candidates AS (
                            SELECT id
                            FROM outbox_events
                            WHERE (status = :pendingStatus OR status = :failedStatus)
                              AND attempts < :maxAttempts
                            ORDER BY created_at ASC
                            LIMIT :limit
                            FOR UPDATE SKIP LOCKED
                        )
                        UPDATE outbox_events event
                        SET status = :processingStatus,
                            attempts = event.attempts + 1,
                            last_attempt_at = :lastAttemptAt,
                            error_message = NULL
                        FROM candidates
                        WHERE event.id = candidates.id
                        RETURNING event.id, event.aggregate_type, event.aggregate_key, event.event_type,
                                  event.kafka_topic, event.kafka_key, event.payload_json, event.status,
                                  event.attempts, event.created_at
                        """)
                .bind("pendingStatus", STATUS_PENDING)
                .bind("failedStatus", STATUS_FAILED)
                .bind("processingStatus", STATUS_PROCESSING)
                .bind("maxAttempts", maxAttempts)
                .bind("limit", limit)
                .bind("lastAttemptAt", Instant.now())
                .map((row, metadata) -> new OutboxRecord(
                        row.get("id", UUID.class),
                        row.get("aggregate_type", String.class),
                        row.get("aggregate_key", String.class),
                        row.get("event_type", String.class),
                        row.get("kafka_topic", String.class),
                        row.get("kafka_key", String.class),
                        RowValueMappers.toJsonString(row.get("payload_json")),
                        row.get("status", String.class),
                        row.get("attempts", Integer.class),
                        RowValueMappers.toInstant(row.get("created_at"))
                ))
                .all();
    }

    /**
     * Marks an event as sent.
     *
     * @param id outbox event id
     * @return completion signal
     */
    public Mono<Void> markSent(final UUID id) {
        return databaseClient.sql("""
                        UPDATE outbox_events
                        SET status = :status, last_attempt_at = :lastAttemptAt, error_message = NULL
                        WHERE id = :id
                        """)
                .bind("id", id)
                .bind("status", STATUS_SENT)
                .bind("lastAttemptAt", Instant.now())
                .fetch()
                .rowsUpdated()
                .then();
    }

    /**
     * Marks an event as failed after a counted publish attempt.
     *
     * @param id outbox event id
     * @param errorMessage sanitized error message
     * @return completion signal
     */
    public Mono<Void> markFailed(final UUID id, final String errorMessage) {
        String safeError = errorMessage == null ? "Unknown Kafka publish error" : errorMessage;
        if (safeError.length() > 1000) {
            safeError = safeError.substring(0, 1000);
        }
        return databaseClient.sql("""
                        UPDATE outbox_events
                        SET status = :status,
                            last_attempt_at = :lastAttemptAt,
                            error_message = :errorMessage
                        WHERE id = :id
                        """)
                .bind("id", id)
                .bind("status", STATUS_FAILED)
                .bind("lastAttemptAt", Instant.now())
                .bind("errorMessage", safeError)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private String toJson(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Outbox payload must be JSON serializable");
        }
    }
}
