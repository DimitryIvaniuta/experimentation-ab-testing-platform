package com.github.dimitryivaniuta.experimentation.service;

import com.github.dimitryivaniuta.experimentation.api.dto.MetricResponse;
import com.github.dimitryivaniuta.experimentation.api.dto.MetricsResponse;
import com.github.dimitryivaniuta.experimentation.api.dto.MetricsSummaryResponse;
import com.github.dimitryivaniuta.experimentation.api.dto.VariantMetricSummaryResponse;
import com.github.dimitryivaniuta.experimentation.domain.MetricRecord;
import com.github.dimitryivaniuta.experimentation.repository.MetricsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Reads aggregated experiment metrics.
 */
@Service
@RequiredArgsConstructor
public class MetricsService {

    private static final int RATE_SCALE = 6;

    private final MetricsRepository metricsRepository;

    /**
     * Returns metrics for one experiment.
     *
     * @param experimentKey experiment key
     * @return metrics response
     */
    public Mono<MetricsResponse> getMetrics(final String experimentKey) {
        return metricsRepository.findByExperiment(experimentKey)
                .map(this::toResponse)
                .collectList()
                .map(metrics -> new MetricsResponse(experimentKey, metrics));
    }

    /**
     * Returns dashboard-friendly exposure, event count, total value, and conversion-rate summaries.
     *
     * @param experimentKey experiment key
     * @return summarized metrics response
     */
    public Mono<MetricsSummaryResponse> getSummary(final String experimentKey) {
        return metricsRepository.findByExperiment(experimentKey)
                .collectList()
                .map(metrics -> new MetricsSummaryResponse(experimentKey, summarize(metrics)));
    }

    private MetricResponse toResponse(final MetricRecord record) {
        BigDecimal average = record.eventCount() == 0
                ? BigDecimal.ZERO
                : record.totalValue().divide(BigDecimal.valueOf(record.eventCount()), RATE_SCALE, RoundingMode.HALF_UP);
        return new MetricResponse(record.variantKey(), record.metricName(), record.eventCount(), record.totalValue(), average);
    }

    private List<VariantMetricSummaryResponse> summarize(final List<MetricRecord> metrics) {
        return metrics.stream()
                .collect(Collectors.groupingBy(MetricRecord::variantKey, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> summarizeVariant(entry.getKey(), entry.getValue()))
                .toList();
    }

    private VariantMetricSummaryResponse summarizeVariant(final String variantKey, final List<MetricRecord> metrics) {
        long exposureCount = metrics.stream()
                .filter(metric -> MetricsRepository.EXPOSURE_METRIC_NAME.equals(metric.metricName()))
                .mapToLong(MetricRecord::eventCount)
                .sum();
        Map<String, Long> eventCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> totalValues = new LinkedHashMap<>();
        Map<String, BigDecimal> conversionRates = new LinkedHashMap<>();
        metrics.stream()
                .filter(metric -> !MetricsRepository.EXPOSURE_METRIC_NAME.equals(metric.metricName()))
                .sorted(Comparator.comparing(MetricRecord::metricName))
                .forEach(metric -> {
                    eventCounts.put(metric.metricName(), metric.eventCount());
                    totalValues.put(metric.metricName(), metric.totalValue());
                    conversionRates.put(metric.metricName(), conversionRate(metric.eventCount(), exposureCount));
                });
        return new VariantMetricSummaryResponse(variantKey, exposureCount, eventCounts, totalValues, conversionRates);
    }

    private BigDecimal conversionRate(final long eventCount, final long exposureCount) {
        if (exposureCount == 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(eventCount).divide(BigDecimal.valueOf(exposureCount), RATE_SCALE, RoundingMode.HALF_UP);
    }
}
