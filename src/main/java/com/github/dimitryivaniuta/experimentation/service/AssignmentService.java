package com.github.dimitryivaniuta.experimentation.service;

import com.github.dimitryivaniuta.experimentation.api.dto.AssignmentResponse;
import com.github.dimitryivaniuta.experimentation.domain.AssignmentRecord;
import com.github.dimitryivaniuta.experimentation.domain.ExperimentDefinition;
import com.github.dimitryivaniuta.experimentation.domain.VariantRecord;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import com.github.dimitryivaniuta.experimentation.exception.NotFoundException;
import com.github.dimitryivaniuta.experimentation.repository.AssignmentRepository;
import com.github.dimitryivaniuta.experimentation.repository.EventRepository;
import com.github.dimitryivaniuta.experimentation.repository.MetricsRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/**
 * Assigns users to variants and records feature flag exposure events.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    /** Reason returned when the experiment cannot currently assign users. */
    public static final String REASON_DISABLED_OR_EMPTY = "EXPERIMENT_DISABLED_OR_NO_VARIANTS";

    /** Reason returned when a user is deterministically outside the configured rollout percentage. */
    public static final String REASON_OUT_OF_TRAFFIC = "OUT_OF_TRAFFIC_ALLOCATION";

    private final ExperimentRepository experimentRepository;
    private final AssignmentRepository assignmentRepository;
    private final EventRepository eventRepository;
    private final MetricsRepository metricsRepository;
    private final OutboxService outboxService;
    private final AssignmentCache assignmentCache;
    private final UserHasher userHasher;
    private final DeterministicVariantAssigner deterministicVariantAssigner;
    private final JsonPayloadMapper jsonPayloadMapper;
    private final TransactionalOperator transactionalOperator;

    /**
     * Returns a stable assignment and records an exposure event.
     *
     * @param experimentKey experiment key
     * @param rawUserId raw user id from request header
     * @return assignment response
     */
    public Mono<AssignmentResponse> assignAndExpose(final String experimentKey, final String rawUserId) {
        return resolveAssignment(experimentKey, rawUserId)
                .flatMap(context -> {
                    if (!context.assigned()) {
                        return Mono.just(context.toResponse(jsonPayloadMapper));
                    }
                    Map<String, Object> metadata = Map.of("source", "flag_assignment_api");
                    return eventRepository.saveExposure(context.experimentKey(), context.userHash(), context.variant().key(), metadata)
                            .flatMap(eventId -> metricsRepository.incrementExposure(context.experimentKey(), context.variant().key())
                                    .then(outboxService.enqueueExposure(eventId.toString(), context.experimentKey(), context.userHash(), context.variant().key())))
                            .thenReturn(context.toResponse(jsonPayloadMapper))
                            .as(transactionalOperator::transactional);
                });
    }

    /**
     * Returns a stable assignment without recording an exposure event.
     *
     * @param experimentKey experiment key
     * @param rawUserId raw user id from event request
     * @return internal assignment context
     */
    public Mono<AssignmentContext> assignSilently(final String experimentKey, final String rawUserId) {
        return resolveAssignment(experimentKey, rawUserId);
    }

    private Mono<AssignmentContext> resolveAssignment(final String experimentKey, final String rawUserId) {
        String userHash = userHasher.hash(rawUserId);
        return experimentRepository.findDefinition(experimentKey)
                .switchIfEmpty(Mono.error(new NotFoundException("Experiment not found")))
                .flatMap(definition -> {
                    if (!definition.isAssignable()) {
                        return Mono.just(AssignmentContext.disabled(experimentKey));
                    }
                    return findExistingAssignment(experimentKey, userHash)
                            .map(record -> AssignmentContext.assigned(
                                    experimentKey, userHash, record, findAssignedVariant(definition, record.variantKey())))
                            .switchIfEmpty(Mono.defer(() -> {
                                if (!deterministicVariantAssigner.isInTraffic(definition, userHash)) {
                                    return Mono.just(AssignmentContext.notAllocated(experimentKey, userHash));
                                }
                                return createAssignment(definition, userHash)
                                        .map(record -> AssignmentContext.assigned(
                                                experimentKey, userHash, record, findAssignedVariant(definition, record.variantKey())));
                            }));
                });
    }

    private Mono<AssignmentRecord> findExistingAssignment(final String experimentKey, final String userHash) {
        return assignmentCache.get(experimentKey, userHash)
                .switchIfEmpty(assignmentRepository.find(experimentKey, userHash).flatMap(assignmentCache::put));
    }

    private Mono<AssignmentRecord> createAssignment(final ExperimentDefinition definition, final String userHash) {
        VariantRecord variant = deterministicVariantAssigner.assign(definition, userHash);
        return assignmentRepository.insertIfAbsent(definition.experiment().key(), userHash, variant.key())
                .as(transactionalOperator::transactional)
                .flatMap(assignmentCache::put);
    }

    private VariantRecord findAssignedVariant(final ExperimentDefinition definition, final String variantKey) {
        return definition.variants().stream()
                .filter(variant -> variant.key().equals(variantKey))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Persisted assignment references a missing variant"));
    }

    /**
     * Internal assignment context used by assignment and tracking flows.
     *
     * @param experimentKey experiment key
     * @param userHash privacy-safe user hash, or {@code null} when the experiment is disabled
     * @param assignment persisted assignment, or {@code null} when no variant is assigned
     * @param variant assigned variant, or {@code null} when no variant is assigned
     * @param assigned whether a variant was assigned
     * @param reason optional non-assignment reason
     */
    public record AssignmentContext(
            String experimentKey,
            String userHash,
            AssignmentRecord assignment,
            VariantRecord variant,
            boolean assigned,
            String reason
    ) {

        /**
         * Creates an assigned context.
         *
         * @param experimentKey experiment key
         * @param userHash privacy-safe user hash
         * @param assignment persisted assignment
         * @param variant assigned variant
         * @return assigned context
         */
        public static AssignmentContext assigned(final String experimentKey, final String userHash,
                                                 final AssignmentRecord assignment, final VariantRecord variant) {
            return new AssignmentContext(experimentKey, userHash, assignment, variant, true, null);
        }

        /**
         * Creates a disabled context.
         *
         * @param experimentKey experiment key
         * @return disabled context
         */
        public static AssignmentContext disabled(final String experimentKey) {
            return new AssignmentContext(experimentKey, null, null, null, false, REASON_DISABLED_OR_EMPTY);
        }

        /**
         * Creates a context for a user outside the configured traffic allocation.
         *
         * @param experimentKey experiment key
         * @param userHash privacy-safe user hash
         * @return not-allocated context
         */
        public static AssignmentContext notAllocated(final String experimentKey, final String userHash) {
            return new AssignmentContext(experimentKey, userHash, null, null, false, REASON_OUT_OF_TRAFFIC);
        }

        /**
         * Converts the context to a client response.
         *
         * @param mapper JSON payload mapper
         * @return assignment response
         */
        public AssignmentResponse toResponse(final JsonPayloadMapper mapper) {
            if (!assigned) {
                return new AssignmentResponse(experimentKey, !REASON_DISABLED_OR_EMPTY.equals(reason), false, null, Map.of(), null, reason);
            }
            return new AssignmentResponse(
                    experimentKey,
                    true,
                    true,
                    variant.key(),
                    mapper.toMap(variant.payloadJson()),
                    assignment.assignedAt(),
                    null
            );
        }
    }
}
