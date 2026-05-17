package com.github.dimitryivaniuta.experimentation.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request used to create a new feature flag experiment.
 *
 * @param key unique experiment key used by clients
 * @param name human-readable experiment name
 * @param enabled whether the experiment should assign traffic
 * @param trafficAllocationBp rollout allocation in basis points from 1 to 10000
 * @param variants weighted variants for users inside the rollout; weights must sum to 10000
 */
public record CreateExperimentRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String key,
        @NotBlank @Size(max = 240) String name,
        boolean enabled,
        @Min(1) @Max(10000) int trafficAllocationBp,
        @Valid @NotEmpty @Size(max = 20) List<CreateVariantRequest> variants
) {
}
