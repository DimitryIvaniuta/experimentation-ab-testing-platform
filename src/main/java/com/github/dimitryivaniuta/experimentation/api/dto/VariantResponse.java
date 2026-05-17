package com.github.dimitryivaniuta.experimentation.api.dto;

import java.util.Map;

/**
 * Public variant response.
 *
 * @param key variant key
 * @param weight variant weight
 * @param payload client-facing JSON payload
 */
public record VariantResponse(String key, int weight, Map<String, Object> payload) {
}
