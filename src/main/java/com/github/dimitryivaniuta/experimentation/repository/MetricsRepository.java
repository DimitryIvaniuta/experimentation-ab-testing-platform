package com.github.dimitryivaniuta.experimentation.repository;

import com.github.dimitryivaniuta.experimentation.domain.MetricRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for per-variant metric aggregation.
 */
@Repository
@RequiredArgsConstructor
public class MetricsRepository {

    /** Metric name reserved for exposure counts. */
    public static final String EXPOSURE_METRIC_NAME = "__exposure__";

    private final DatabaseClient databaseClient;

    /**
     * Increments the reserved exposure metric by one.
     *
     * @param experimentKey experiment key
     * @param variantKey variant key
     * @return completion signal
     */
    public Mono<Void> incrementExposure(final String experimentKey, final String variantKey) {
        return increment(experimentKey, variantKey, EXPOSURE_METRIC_NAME, BigDecimal.ZERO);
    }

    /**
     * Increments a custom metric by one and adds its numeric value.
     *
     * @param experimentKey experiment key
     * @param variantKey variant key
     * @param metricName metric name
     * @param value numeric event value
     * @return completion signal
     */
    public Mono<Void> increment(final String experimentKey, final String variantKey, final String metricName, final BigDecimal value) {
        return databaseClient.sql("""
                        INSERT INTO variant_metrics (id, experiment_key, variant_key, metric_name, event_count, total_value, last_updated_at)
                        VALUES (:id, :experimentKey, :variantKey, :metricName, 1, :value, :lastUpdatedAt)
                        ON CONFLICT (experiment_key, variant_key, metric_name)
                        DO UPDATE SET
                            event_count = variant_metrics.event_count + 1,
                            total_value = variant_metrics.total_value + EXCLUDED.total_value,
                            last_updated_at = EXCLUDED.last_updated_at
                        """)
                .bind("id", UUID.randomUUID())
                .bind("experimentKey", experimentKey)
                .bind("variantKey", variantKey)
                .bind("metricName", metricName)
                .bind("value", value)
                .bind("lastUpdatedAt", Instant.now())
                .fetch()
                .rowsUpdated()
                .then();
    }

    /**
     * Finds all aggregated metrics for one experiment.
     *
     * @param experimentKey experiment key
     * @return metrics ordered by variant and metric name
     */
    public Flux<MetricRecord> findByExperiment(final String experimentKey) {
        return databaseClient.sql("""
                        SELECT id, experiment_key, variant_key, metric_name, event_count, total_value, last_updated_at
                        FROM variant_metrics
                        WHERE experiment_key = :experimentKey
                        ORDER BY variant_key ASC, metric_name ASC
                        """)
                .bind("experimentKey", experimentKey)
                .map((row, metadata) -> new MetricRecord(
                        row.get("id", UUID.class),
                        row.get("experiment_key", String.class),
                        row.get("variant_key", String.class),
                        row.get("metric_name", String.class),
                        row.get("event_count", Long.class),
                        row.get("total_value", BigDecimal.class),
                        RowValueMappers.toInstant(row.get("last_updated_at"))
                ))
                .all();
    }
}
