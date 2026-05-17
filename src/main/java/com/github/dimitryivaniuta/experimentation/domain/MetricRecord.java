package com.github.dimitryivaniuta.experimentation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregated metric row for one experiment, variant, and metric name.
 *
 * @param id metric identifier
 * @param experimentKey experiment key
 * @param variantKey variant key
 * @param metricName metric name, for example {@code __exposure__} or {@code purchase}
 * @param eventCount count of events
 * @param totalValue sum of numeric event values
 * @param lastUpdatedAt last update timestamp
 */
public record MetricRecord(
        UUID id,
        String experimentKey,
        String variantKey,
        String metricName,
        long eventCount,
        BigDecimal totalValue,
        Instant lastUpdatedAt
) {
}
