package com.github.dimitryivaniuta.experimentation.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import com.github.dimitryivaniuta.experimentation.service.MetadataPrivacySanitizer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for exposure and tracking event persistence.
 */
@Repository
@RequiredArgsConstructor
public class EventRepository {

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;
    private final MetadataPrivacySanitizer metadataPrivacySanitizer;

    /**
     * Persists one exposure event.
     *
     * @param experimentKey experiment key
     * @param userHash privacy-safe user hash
     * @param variantKey variant key
     * @param metadata non-PII metadata
     * @return saved event identifier
     */
    public Mono<UUID> saveExposure(final String experimentKey, final String userHash, final String variantKey,
                                   final Map<String, Object> metadata) {
        UUID id = UUID.randomUUID();
        return databaseClient.sql("""
                        INSERT INTO exposure_events (id, experiment_key, user_hash, variant_key, occurred_at, metadata_json)
                        VALUES (:id, :experimentKey, :userHash, :variantKey, :occurredAt, CAST(:metadataJson AS jsonb))
                        """)
                .bind("id", id)
                .bind("experimentKey", experimentKey)
                .bind("userHash", userHash)
                .bind("variantKey", variantKey)
                .bind("occurredAt", Instant.now())
                .bind("metadataJson", toJson(metadataPrivacySanitizer.sanitize(metadata)))
                .fetch()
                .rowsUpdated()
                .thenReturn(id);
    }

    /**
     * Persists one custom tracking event.
     *
     * @param experimentKey experiment key
     * @param userHash privacy-safe user hash
     * @param variantKey variant key
     * @param eventName event name
     * @param value numeric event value
     * @param metadata non-PII metadata
     * @return saved event identifier
     */
    public Mono<UUID> saveTracking(final String experimentKey, final String userHash, final String variantKey,
                                   final String eventName, final BigDecimal value, final Map<String, Object> metadata) {
        UUID id = UUID.randomUUID();
        return databaseClient.sql("""
                        INSERT INTO tracking_events (id, experiment_key, user_hash, variant_key, event_name, event_value, occurred_at, metadata_json)
                        VALUES (:id, :experimentKey, :userHash, :variantKey, :eventName, :eventValue, :occurredAt, CAST(:metadataJson AS jsonb))
                        """)
                .bind("id", id)
                .bind("experimentKey", experimentKey)
                .bind("userHash", userHash)
                .bind("variantKey", variantKey)
                .bind("eventName", eventName)
                .bind("eventValue", value)
                .bind("occurredAt", Instant.now())
                .bind("metadataJson", toJson(metadataPrivacySanitizer.sanitize(metadata)))
                .fetch()
                .rowsUpdated()
                .thenReturn(id);
    }

    private String toJson(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Event metadata must be JSON serializable");
        }
    }
}
