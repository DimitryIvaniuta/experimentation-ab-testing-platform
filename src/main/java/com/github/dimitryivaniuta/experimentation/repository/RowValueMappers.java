package com.github.dimitryivaniuta.experimentation.repository;

import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Utility methods for safe R2DBC row value conversions across PostgreSQL driver versions.
 */
final class RowValueMappers {

    private RowValueMappers() {
    }

    /**
     * Converts PostgreSQL timestamp values into {@link Instant}.
     *
     * @param value row value
     * @return instant value
     */
    static Instant toInstant(final Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        throw new IllegalArgumentException("Unsupported timestamp value: " + value);
    }

    /**
     * Converts PostgreSQL JSON values into raw JSON text.
     *
     * @param value row value
     * @return JSON string
     */
    static String toJsonString(final Object value) {
        if (value instanceof Json json) {
            return json.asString();
        }
        return String.valueOf(value);
    }
}
