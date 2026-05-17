package com.github.dimitryivaniuta.experimentation.service;

import com.github.dimitryivaniuta.experimentation.config.ExperimentationProperties;
import com.github.dimitryivaniuta.experimentation.repository.OutboxRepository;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Creates privacy-safe outbox events for asynchronous Kafka publication.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ExperimentationProperties properties;

    /**
     * Adds one exposure event to the outbox.
     *
     * @param eventId persisted exposure event id
     * @param experimentKey experiment key
     * @param userHash privacy-safe user hash
     * @param variantKey variant key
     * @return completion signal
     */
    public Mono<Void> enqueueExposure(final String eventId, final String experimentKey, final String userHash, final String variantKey) {
        Map<String, Object> payload = Map.of(
                "eventId", eventId,
                "eventType", "variant_exposed",
                "experimentKey", experimentKey,
                "userHash", userHash,
                "variantKey", variantKey,
                "occurredAt", Instant.now().toString()
        );
        return outboxRepository.savePending(
                        "exposure", eventId, "variant_exposed", properties.kafka().exposureTopic(), experimentKey + ':' + userHash, payload)
                .then();
    }

    /**
     * Adds one tracking event to the outbox.
     *
     * @param eventId persisted tracking event id
     * @param experimentKey experiment key
     * @param userHash privacy-safe user hash
     * @param variantKey variant key
     * @param eventName custom event name
     * @return completion signal
     */
    public Mono<Void> enqueueTracking(final String eventId, final String experimentKey, final String userHash,
                                      final String variantKey, final String eventName) {
        Map<String, Object> payload = Map.of(
                "eventId", eventId,
                "eventType", "experiment_event_tracked",
                "experimentKey", experimentKey,
                "userHash", userHash,
                "variantKey", variantKey,
                "eventName", eventName,
                "occurredAt", Instant.now().toString()
        );
        return outboxRepository.savePending(
                        "tracking", eventId, "experiment_event_tracked", properties.kafka().trackingTopic(), experimentKey + ':' + userHash, payload)
                .then();
    }
}
