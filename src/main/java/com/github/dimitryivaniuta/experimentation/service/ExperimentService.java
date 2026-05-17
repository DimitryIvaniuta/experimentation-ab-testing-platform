package com.github.dimitryivaniuta.experimentation.service;

import com.github.dimitryivaniuta.experimentation.api.dto.CreateExperimentRequest;
import com.github.dimitryivaniuta.experimentation.api.dto.CreateVariantRequest;
import com.github.dimitryivaniuta.experimentation.api.dto.ExperimentResponse;
import com.github.dimitryivaniuta.experimentation.api.dto.VariantResponse;
import com.github.dimitryivaniuta.experimentation.domain.ExperimentDefinition;
import com.github.dimitryivaniuta.experimentation.domain.VariantRecord;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import com.github.dimitryivaniuta.experimentation.exception.NotFoundException;
import com.github.dimitryivaniuta.experimentation.repository.ExperimentRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/**
 * Application service for experiment administration and read models.
 */
@Service
@RequiredArgsConstructor
public class ExperimentService {

    private static final int FULL_VARIANT_WEIGHT = 10_000;

    private final ExperimentRepository experimentRepository;
    private final JsonPayloadMapper jsonPayloadMapper;
    private final TransactionalOperator transactionalOperator;

    /**
     * Creates a new experiment after validating variant weights and keys.
     *
     * @param request create request
     * @return created experiment response
     */
    public Mono<ExperimentResponse> create(final CreateExperimentRequest request) {
        validate(request);
        return experimentRepository.create(request)
                .as(transactionalOperator::transactional)
                .map(this::toResponse);
    }

    /**
     * Returns an experiment definition by key.
     *
     * @param key experiment key
     * @return experiment response
     */
    public Mono<ExperimentResponse> get(final String key) {
        return experimentRepository.findDefinition(key)
                .switchIfEmpty(Mono.error(new NotFoundException("Experiment not found")))
                .map(this::toResponse);
    }

    private void validate(final CreateExperimentRequest request) {
        Set<String> keys = new HashSet<>();
        int totalWeight = 0;
        for (CreateVariantRequest variant : request.variants()) {
            if (!keys.add(variant.key())) {
                throw new BusinessRuleException("Variant keys must be unique");
            }
            totalWeight += variant.weight();
        }
        if (totalWeight != FULL_VARIANT_WEIGHT) {
            throw new BusinessRuleException("Sum of variant weights must equal 10000 basis points");
        }
    }

    private ExperimentResponse toResponse(final ExperimentDefinition definition) {
        List<VariantResponse> variants = definition.variants().stream()
                .map(this::toVariantResponse)
                .toList();
        return new ExperimentResponse(
                definition.experiment().key(),
                definition.experiment().name(),
                definition.experiment().enabled(),
                definition.experiment().trafficAllocationBp(),
                variants,
                definition.experiment().createdAt(),
                definition.experiment().updatedAt()
        );
    }

    private VariantResponse toVariantResponse(final VariantRecord variant) {
        return new VariantResponse(variant.key(), variant.weight(), jsonPayloadMapper.toMap(variant.payloadJson()));
    }
}
