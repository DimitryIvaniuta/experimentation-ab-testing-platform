package com.github.dimitryivaniuta.experimentation.repository;

import com.github.dimitryivaniuta.experimentation.api.dto.CreateExperimentRequest;
import com.github.dimitryivaniuta.experimentation.api.dto.CreateVariantRequest;
import com.github.dimitryivaniuta.experimentation.domain.ExperimentDefinition;
import com.github.dimitryivaniuta.experimentation.domain.ExperimentRecord;
import com.github.dimitryivaniuta.experimentation.domain.VariantRecord;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive PostgreSQL repository for experiment definitions and variants.
 */
@Repository
@RequiredArgsConstructor
public class ExperimentRepository {

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    /**
     * Finds one experiment and all variants by public experiment key.
     *
     * @param key experiment key
     * @return complete experiment definition when found
     */
    public Mono<ExperimentDefinition> findDefinition(final String key) {
        return findExperiment(key)
                .flatMap(experiment -> findVariants(experiment.id())
                        .collectList()
                        .map(variants -> new ExperimentDefinition(experiment, variants)));
    }

    /**
     * Creates an experiment and all its variants.
     *
     * @param request validated create request
     * @return created experiment definition
     */
    public Mono<ExperimentDefinition> create(final CreateExperimentRequest request) {
        UUID experimentId = UUID.randomUUID();
        String salt = UUID.randomUUID().toString();
        Instant now = Instant.now();
        return databaseClient.sql("""
                        INSERT INTO experiments (id, experiment_key, name, enabled, salt, traffic_allocation_bp, created_at, updated_at)
                        VALUES (:id, :experimentKey, :name, :enabled, :salt, :trafficAllocationBp, :createdAt, :updatedAt)
                        """)
                .bind("id", experimentId)
                .bind("experimentKey", request.key())
                .bind("name", request.name())
                .bind("enabled", request.enabled())
                .bind("salt", salt)
                .bind("trafficAllocationBp", request.trafficAllocationBp())
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .fetch()
                .rowsUpdated()
                .onErrorMap(DuplicateKeyException.class, duplicate -> new BusinessRuleException("Experiment key already exists"))
                .thenMany(insertVariants(experimentId, request.variants()))
                .then(findDefinition(request.key()));
    }

    private Mono<ExperimentRecord> findExperiment(final String key) {
        return databaseClient.sql("""
                        SELECT id, experiment_key, name, enabled, salt, traffic_allocation_bp, created_at, updated_at
                        FROM experiments
                        WHERE experiment_key = :experimentKey
                        """)
                .bind("experimentKey", key)
                .map((row, metadata) -> new ExperimentRecord(
                        row.get("id", UUID.class),
                        row.get("experiment_key", String.class),
                        row.get("name", String.class),
                        Boolean.TRUE.equals(row.get("enabled", Boolean.class)),
                        row.get("salt", String.class),
                        row.get("traffic_allocation_bp", Integer.class),
                        RowValueMappers.toInstant(row.get("created_at")),
                        RowValueMappers.toInstant(row.get("updated_at"))
                ))
                .one();
    }

    private Flux<VariantRecord> findVariants(final UUID experimentId) {
        return databaseClient.sql("""
                        SELECT id, experiment_id, variant_key, weight, payload_json, position
                        FROM variants
                        WHERE experiment_id = :experimentId
                        ORDER BY position ASC
                        """)
                .bind("experimentId", experimentId)
                .map((row, metadata) -> new VariantRecord(
                        row.get("id", UUID.class),
                        row.get("experiment_id", UUID.class),
                        row.get("variant_key", String.class),
                        row.get("weight", Integer.class),
                        RowValueMappers.toJsonString(row.get("payload_json")),
                        row.get("position", Integer.class)
                ))
                .all();
    }

    private Flux<Long> insertVariants(final UUID experimentId, final List<CreateVariantRequest> variants) {
        return Flux.range(0, variants.size())
                .concatMap(position -> {
                    CreateVariantRequest variant = variants.get(position);
                    return databaseClient.sql("""
                                    INSERT INTO variants (id, experiment_id, variant_key, weight, payload_json, position)
                                    VALUES (:id, :experimentId, :variantKey, :weight, CAST(:payloadJson AS jsonb), :position)
                                    """)
                            .bind("id", UUID.randomUUID())
                            .bind("experimentId", experimentId)
                            .bind("variantKey", variant.key())
                            .bind("weight", variant.weight())
                            .bind("payloadJson", toJson(variant.payload()))
                            .bind("position", position)
                            .fetch()
                            .rowsUpdated();
                });
    }

    private String toJson(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("Variant payload must be JSON serializable");
        }
    }
}
