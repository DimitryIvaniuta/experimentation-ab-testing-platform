package com.github.dimitryivaniuta.experimentation.api.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Variant-level metric summary optimized for dashboards.
 *
 * @param variantKey variant key
 * @param exposureCount number of exposure events recorded for the variant
 * @param eventCountsByMetric event count by custom metric name
 * @param totalValuesByMetric total numeric value by custom metric name
 * @param conversionRatesByMetric event count divided by exposure count for each custom metric
 */
public record VariantMetricSummaryResponse(
        String variantKey,
        long exposureCount,
        Map<String, Long> eventCountsByMetric,
        Map<String, BigDecimal> totalValuesByMetric,
        Map<String, BigDecimal> conversionRatesByMetric
) {
}
