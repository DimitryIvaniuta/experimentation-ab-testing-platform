package com.github.dimitryivaniuta.experimentation.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted assignment row guaranteeing stable assignment even if weights change later.
 *
 * @param id assignment identifier
 * @param experimentKey experiment key
 * @param userHash privacy-safe HMAC hash of the raw user id
 * @param variantKey assigned variant key
 * @param assignedAt assignment timestamp
 */
public record AssignmentRecord(UUID id, String experimentKey, String userHash, String variantKey, Instant assignedAt) {
}
