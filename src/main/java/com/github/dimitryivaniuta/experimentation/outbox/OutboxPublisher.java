package com.github.dimitryivaniuta.experimentation.outbox;

import com.github.dimitryivaniuta.experimentation.config.ExperimentationProperties;
import com.github.dimitryivaniuta.experimentation.domain.OutboxRecord;
import com.github.dimitryivaniuta.experimentation.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Scheduled publisher that moves pending outbox events to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final ExperimentationProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Polls publishable outbox rows and publishes them to Kafka.
     */
    @Scheduled(fixedDelayString = "${experimentation.outbox.poll-delay-ms:1000}")
    public void publishPendingEvents() {
        if (!properties.outbox().enabled()) {
            return;
        }
        outboxRepository.claimPublishable(properties.outbox().batchSize(), properties.outbox().maxAttempts())
                .concatMap(this::publishOne)
                .onErrorResume(error -> {
                    log.warn("Outbox polling failed: {}", error.getMessage());
                    return Flux.empty();
                })
                .subscribe();
    }

    private Mono<Void> publishOne(final OutboxRecord event) {
        return Mono.fromFuture(kafkaTemplate.send(event.kafkaTopic(), event.kafkaKey(), event.payloadJson()))
                .flatMap(result -> outboxRepository.markSent(event.id()))
                .doOnSuccess(ignored -> log.debug("Published outbox event {} to topic {}", event.id(), event.kafkaTopic()))
                .onErrorResume(error -> outboxRepository.markFailed(event.id(), error.getMessage()));
    }
}
