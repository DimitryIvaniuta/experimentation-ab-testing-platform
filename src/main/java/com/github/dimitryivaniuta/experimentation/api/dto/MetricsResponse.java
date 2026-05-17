package com.github.dimitryivaniuta.experimentation.api.dto;

import java.util.List;

/**
 * Metrics response for one experiment.
 *
 * @param experimentKey experiment key
 * @param metrics metrics grouped by variant and metric name
 */
public record MetricsResponse(String experimentKey, List<MetricResponse> metrics) {
}
