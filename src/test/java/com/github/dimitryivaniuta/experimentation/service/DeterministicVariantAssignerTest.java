package com.github.dimitryivaniuta.experimentation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dimitryivaniuta.experimentation.domain.ExperimentDefinition;
import com.github.dimitryivaniuta.experimentation.domain.ExperimentRecord;
import com.github.dimitryivaniuta.experimentation.domain.VariantRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for deterministic weighted variant assignment.
 */
class DeterministicVariantAssignerTest {

    private final DeterministicVariantAssigner assigner = new DeterministicVariantAssigner();

    /**
     * Verifies that the same user hash always maps to the same variant.
     */
    @Test
    void shouldAssignSameUserHashToSameVariant() {
        ExperimentDefinition definition = definition();
        VariantRecord first = assigner.assign(definition, "same-user-hash");
        VariantRecord second = assigner.assign(definition, "same-user-hash");

        assertThat(second.key()).isEqualTo(first.key());
    }

    /**
     * Verifies that assignment always returns one of configured variants.
     */
    @Test
    void shouldAssignConfiguredVariantOnly() {
        ExperimentDefinition definition = definition();

        VariantRecord selected = assigner.assign(definition, "another-user-hash");

        assertThat(selected.key()).isIn("control", "treatment");
    }

    /**
     * Verifies that traffic allocation is a separate stable decision from variant selection.
     */
    @Test
    void shouldDeterministicallyHoldOutUsersOutsideTrafficAllocation() {
        ExperimentDefinition definition = definitionWithTrafficAllocation(1);
        String heldOutUser = java.util.stream.IntStream.range(0, 20_000)
                .mapToObj(index -> "user-hash-" + index)
                .filter(userHash -> !assigner.isInTraffic(definition, userHash))
                .findFirst()
                .orElseThrow();

        assertThat(assigner.isInTraffic(definition, heldOutUser)).isFalse();
    }

    /**
     * Verifies that full allocation includes every user hash.
     */
    @Test
    void shouldIncludeAllUsersWhenTrafficAllocationIsFull() {
        ExperimentDefinition definition = definitionWithTrafficAllocation(10_000);

        assertThat(assigner.isInTraffic(definition, "any-user-hash")).isTrue();
    }

    private ExperimentDefinition definition() {
        return definitionWithTrafficAllocation(10_000);
    }

    private ExperimentDefinition definitionWithTrafficAllocation(final int trafficAllocationBp) {
        UUID experimentId = UUID.randomUUID();
        ExperimentRecord experiment = new ExperimentRecord(
                experimentId,
                "checkout",
                "Checkout",
                true,
                "checkout-v1",
                trafficAllocationBp,
                Instant.now(),
                Instant.now()
        );
        List<VariantRecord> variants = List.of(
                new VariantRecord(UUID.randomUUID(), experimentId, "control", 5000, "{}", 0),
                new VariantRecord(UUID.randomUUID(), experimentId, "treatment", 5000, "{}", 1)
        );
        return new ExperimentDefinition(experiment, variants);
    }
}
