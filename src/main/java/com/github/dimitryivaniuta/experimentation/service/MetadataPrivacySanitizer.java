package com.github.dimitryivaniuta.experimentation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Sanitizes caller-provided event metadata before persistence or Kafka publication.
 *
 * <p>The platform never stores raw user identifiers. This sanitizer adds a second privacy guard for arbitrary metadata
 * supplied by clients by redacting common PII-like keys while preserving safe business dimensions such as currency,
 * page, source, plan, or device family.</p>
 */
@Component
public class MetadataPrivacySanitizer {

    private static final int MAX_DEPTH = 3;
    private static final int MAX_COLLECTION_ITEMS = 50;
    private static final String REDACTED = "[REDACTED]";

    /**
     * Returns a sanitized copy of metadata.
     *
     * @param metadata caller-provided metadata map, may be {@code null}
     * @return sanitized metadata map without obvious PII values
     */
    public Map<String, Object> sanitize(final Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> sanitized.put(key, sanitizeValue(key, value, 0)));
        return Collections.unmodifiableMap(sanitized);
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeValue(final String key, final Object value, final int depth) {
        if (isSensitiveKey(key)) {
            return REDACTED;
        }
        if (value == null || depth >= MAX_DEPTH) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count++ >= MAX_COLLECTION_ITEMS) {
                    break;
                }
                String nestedKey = String.valueOf(entry.getKey());
                nested.put(nestedKey, sanitizeValue(nestedKey, entry.getValue(), depth + 1));
            }
            return nested;
        }
        if (value instanceof List<?> list) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : list.stream().limit(MAX_COLLECTION_ITEMS).toList()) {
                if (item instanceof Map<?, ?> itemMap) {
                    sanitized.add(sanitizeValue(key, (Map<String, Object>) itemMap, depth + 1));
                } else {
                    sanitized.add(item);
                }
            }
            return sanitized;
        }
        return value;
    }

    private boolean isSensitiveKey(final String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.contains("email")
                || normalized.contains("userid")
                || normalized.equals("user")
                || normalized.contains("phone")
                || normalized.contains("address")
                || normalized.contains("firstname")
                || normalized.contains("lastname")
                || normalized.contains("fullname")
                || normalized.contains("ipaddress")
                || normalized.equals("ip")
                || normalized.contains("cookie")
                || normalized.contains("session")
                || normalized.contains("token");
    }
}
