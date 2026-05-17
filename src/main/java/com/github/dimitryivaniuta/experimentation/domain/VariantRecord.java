package com.github.dimitryivaniuta.experimentation.domain;

import java.util.UUID;

/**
 * Variant row mapped from PostgreSQL.
 *
 * @param id stable variant identifier
 * @param experimentId parent experiment identifier
 * @param key public variant key
 * @param weight assignment weight
 * @param payloadJson JSON payload returned to clients
 * @param position stable variant order
 */
public record VariantRecord(UUID id, UUID experimentId, String key, int weight, String payloadJson, int position) {
}
