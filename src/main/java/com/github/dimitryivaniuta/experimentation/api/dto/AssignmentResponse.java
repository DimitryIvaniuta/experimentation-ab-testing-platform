package com.github.dimitryivaniuta.experimentation.api.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Feature flag assignment response returned to clients.
 *
 * @param experimentKey experiment key
 * @param enabled whether the experiment is enabled for assignment evaluation
 * @param assigned whether a variant was assigned
 * @param variantKey assigned variant key, or {@code null} when no variant is assigned
 * @param payload client-facing variant payload
 * @param assignedAt assignment timestamp, or {@code null} when no variant is assigned
 * @param reason optional machine-readable reason when {@code assigned} is {@code false}
 */
public record AssignmentResponse(
        String experimentKey,
        boolean enabled,
        boolean assigned,
        String variantKey,
        Map<String, Object> payload,
        Instant assignedAt,
        String reason
) {
}
