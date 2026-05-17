package com.github.dimitryivaniuta.experimentation.api;

import com.github.dimitryivaniuta.experimentation.api.dto.MetricsResponse;
import com.github.dimitryivaniuta.experimentation.api.dto.MetricsSummaryResponse;
import com.github.dimitryivaniuta.experimentation.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for aggregated experiment metrics.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    /**
     * Reads raw aggregated metrics for one experiment.
     *
     * @param experimentKey experiment key
     * @return metrics response
     */
    @GetMapping("/{experimentKey}")
    public Mono<MetricsResponse> getMetrics(@PathVariable final String experimentKey) {
        return metricsService.getMetrics(experimentKey);
    }

    /**
     * Reads dashboard-friendly summarized metrics for one experiment.
     *
     * @param experimentKey experiment key
     * @return metrics summary response
     */
    @GetMapping("/{experimentKey}/summary")
    public Mono<MetricsSummaryResponse> getSummary(@PathVariable final String experimentKey) {
        return metricsService.getSummary(experimentKey);
    }
}
