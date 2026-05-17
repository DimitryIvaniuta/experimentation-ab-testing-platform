package com.github.dimitryivaniuta.experimentation.api.dto;

import java.math.BigDecimal;

/**
 * Aggregated metric response.
 *
 * @param variantKey variant key
 * @param metricName metric name
 * @param eventCount number of accepted events
 * @param totalValue total numeric value
 * @param averageValue average numeric value
 */
public record MetricResponse(
        String variantKey,
        String metricName,
        long eventCount,
        BigDecimal totalValue,
        BigDecimal averageValue
) {
}
