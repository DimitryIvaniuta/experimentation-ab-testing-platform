package com.github.dimitryivaniuta.experimentation.domain;

import java.util.List;

/**
 * Complete experiment definition used for assignment decisions.
 *
 * @param experiment experiment metadata and enabled state
 * @param variants ordered weighted variants
 */
public record ExperimentDefinition(ExperimentRecord experiment, List<VariantRecord> variants) {

    /**
     * Returns {@code true} when the experiment can assign users.
     *
     * @return enabled flag with at least one variant
     */
    public boolean isAssignable() {
        return experiment.enabled() && !variants.isEmpty();
    }
}
