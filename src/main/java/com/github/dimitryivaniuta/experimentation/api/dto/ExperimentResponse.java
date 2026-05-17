package com.github.dimitryivaniuta.experimentation.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * Public experiment response.
 *
 * @param key experiment key
 * @param name experiment name
 * @param enabled enabled flag
 * @param trafficAllocationBp traffic allocation in basis points
 * @param variants variant responses
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 */
public record ExperimentResponse(
        String key,
        String name,
        boolean enabled,
        int trafficAllocationBp,
        List<VariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {
}
