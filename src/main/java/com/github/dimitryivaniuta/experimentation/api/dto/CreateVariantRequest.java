package com.github.dimitryivaniuta.experimentation.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Request for one weighted experiment variant.
 *
 * @param key unique variant key within the experiment
 * @param weight assignment weight in basis points within eligible rollout traffic
 * @param payload client-facing JSON payload
 */
public record CreateVariantRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String key,
        @Min(1) @Max(10000) int weight,
        @NotNull Map<String, Object> payload
) {
}
