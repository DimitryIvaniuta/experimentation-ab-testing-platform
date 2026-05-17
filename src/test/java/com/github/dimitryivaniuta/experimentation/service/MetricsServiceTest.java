package com.github.dimitryivaniuta.experimentation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.dimitryivaniuta.experimentation.domain.MetricRecord;
import com.github.dimitryivaniuta.experimentation.repository.MetricsRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Unit tests for aggregated metrics read model behavior.
 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private MetricsRepository metricsRepository;

    /**
     * Verifies average value calculation for aggregated metrics.
     */
    @Test
    void shouldCalculateAverageValue() {
        when(metricsRepository.findByExperiment("checkout"))
                .thenReturn(Flux.just(new MetricRecord(
                        UUID.randomUUID(), "checkout", "control", "purchase", 2, new BigDecimal("30.00"), Instant.now()
                )));
        MetricsService service = new MetricsService(metricsRepository);

        StepVerifier.create(service.getMetrics("checkout"))
                .assertNext(response -> {
                    assertThat(response.metrics()).hasSize(1);
                    assertThat(response.metrics().getFirst().averageValue()).isEqualByComparingTo("15.000000");
                })
                .verifyComplete();
    }

    /**
     * Verifies conversion-rate calculation for dashboard summaries.
     */
    @Test
    void shouldCalculateConversionRatesInSummary() {
        when(metricsRepository.findByExperiment("checkout"))
                .thenReturn(Flux.just(
                        new MetricRecord(UUID.randomUUID(), "checkout", "control", "__exposure__", 10, BigDecimal.ZERO, Instant.now()),
                        new MetricRecord(UUID.randomUUID(), "checkout", "control", "purchase", 2, new BigDecimal("30.00"), Instant.now())
                ));
        MetricsService service = new MetricsService(metricsRepository);

        StepVerifier.create(service.getSummary("checkout"))
                .assertNext(response -> {
                    assertThat(response.variants()).hasSize(1);
                    assertThat(response.variants().getFirst().exposureCount()).isEqualTo(10);
                    assertThat(response.variants().getFirst().conversionRatesByMetric().get("purchase"))
                            .isEqualByComparingTo("0.200000");
                })
                .verifyComplete();
    }

}
