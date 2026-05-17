package com.github.dimitryivaniuta.experimentation.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Experiment row mapped from PostgreSQL.
 *
 * @param id stable experiment identifier
 * @param key public feature flag key
 * @param name human-readable experiment name
 * @param enabled whether assignment is active
 * @param salt per-experiment salt for assignment hashing
 * @param trafficAllocationBp traffic allocation in basis points
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 */
public record ExperimentRecord(
        UUID id,
        String key,
        String name,
        boolean enabled,
        String salt,
        int trafficAllocationBp,
        Instant createdAt,
        Instant updatedAt
) {
}
