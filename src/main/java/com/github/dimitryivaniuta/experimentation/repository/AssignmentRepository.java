package com.github.dimitryivaniuta.experimentation.repository;

import com.github.dimitryivaniuta.experimentation.domain.AssignmentRecord;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Reactive PostgreSQL repository for persisted user assignments.
 */
@Repository
@RequiredArgsConstructor
public class AssignmentRepository {

    private final DatabaseClient databaseClient;

    /**
     * Finds a stable assignment by experiment and privacy-safe user hash.
     *
     * @param experimentKey experiment key
     * @param userHash HMAC-SHA256 user hash
     * @return assignment when present
     */
    public Mono<AssignmentRecord> find(final String experimentKey, final String userHash) {
        return databaseClient.sql("""
                        SELECT id, experiment_key, user_hash, variant_key, assigned_at
                        FROM assignments
                        WHERE experiment_key = :experimentKey AND user_hash = :userHash
                        """)
                .bind("experimentKey", experimentKey)
                .bind("userHash", userHash)
                .map((row, metadata) -> new AssignmentRecord(
                        row.get("id", UUID.class),
                        row.get("experiment_key", String.class),
                        row.get("user_hash", String.class),
                        row.get("variant_key", String.class),
                        RowValueMappers.toInstant(row.get("assigned_at"))
                ))
                .one();
    }

    /**
     * Inserts a stable assignment idempotently and returns the stored row.
     *
     * @param experimentKey experiment key
     * @param userHash HMAC-SHA256 user hash
     * @param variantKey selected variant key
     * @return persisted assignment
     */
    public Mono<AssignmentRecord> insertIfAbsent(final String experimentKey, final String userHash, final String variantKey) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        return databaseClient.sql("""
                        INSERT INTO assignments (id, experiment_key, user_hash, variant_key, assigned_at)
                        VALUES (:id, :experimentKey, :userHash, :variantKey, :assignedAt)
                        ON CONFLICT (experiment_key, user_hash) DO NOTHING
                        """)
                .bind("id", id)
                .bind("experimentKey", experimentKey)
                .bind("userHash", userHash)
                .bind("variantKey", variantKey)
                .bind("assignedAt", now)
                .fetch()
                .rowsUpdated()
                .then(find(experimentKey, userHash));
    }
}
