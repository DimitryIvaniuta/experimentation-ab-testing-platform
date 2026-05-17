package com.github.dimitryivaniuta.experimentation.service;

import com.github.dimitryivaniuta.experimentation.api.dto.TrackEventRequest;
import com.github.dimitryivaniuta.experimentation.api.dto.TrackEventResponse;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import com.github.dimitryivaniuta.experimentation.repository.EventRepository;
import com.github.dimitryivaniuta.experimentation.repository.MetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/**
 * Tracks custom events and updates per-variant metric aggregation.
 */
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final AssignmentService assignmentService;
    private final EventRepository eventRepository;
    private final MetricsRepository metricsRepository;
    private final OutboxService outboxService;
    private final TransactionalOperator transactionalOperator;

    /**
     * Accepts a tracking event, resolves the user's variant, persists the event, and updates metrics.
     *
     * @param request tracking request
     * @return accepted tracking response
     */
    public Mono<TrackEventResponse> track(final TrackEventRequest request) {
        return assignmentService.assignSilently(request.experimentKey(), request.userId())
                .flatMap(context -> {
                    if (!context.assigned()) {
                        return Mono.error(new BusinessRuleException("Experiment is disabled or has no variants"));
                    }
                    return eventRepository.saveTracking(
                                    context.experimentKey(),
                                    context.userHash(),
                                    context.variant().key(),
                                    request.eventName(),
                                    request.value(),
                                    request.metadata()
                            )
                            .flatMap(eventId -> metricsRepository.increment(
                                            context.experimentKey(), context.variant().key(), request.eventName(), request.value())
                                    .then(outboxService.enqueueTracking(
                                            eventId.toString(), context.experimentKey(), context.userHash(), context.variant().key(), request.eventName())))
                            .thenReturn(new TrackEventResponse(true, context.experimentKey(), context.variant().key(), request.eventName()))
                            .as(transactionalOperator::transactional);
                });
    }
}
