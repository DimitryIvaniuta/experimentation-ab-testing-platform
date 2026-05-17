package com.github.dimitryivaniuta.experimentation.api.dto;

import java.util.List;

/**
 * Dashboard-friendly metrics summary for one experiment.
 *
 * @param experimentKey experiment key
 * @param variants variant-level metric summaries
 */
public record MetricsSummaryResponse(String experimentKey, List<VariantMetricSummaryResponse> variants) {
}
