package com.github.dimitryivaniuta.experimentation.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Request used to track a custom event or conversion.
 *
 * @param experimentKey experiment key
 * @param userId raw user id from trusted caller; it is hashed before storage
 * @param eventName event name, for example {@code purchase} or {@code signup}
 * @param value numeric event value aggregated per variant
 * @param metadata optional non-PII metadata
 */
public record TrackEventRequest(
        @NotBlank @Size(max = 120) String experimentKey,
        @NotBlank @Size(max = 240) String userId,
        @NotBlank @Size(max = 160) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String eventName,
        @NotNull @DecimalMin(value = "0.0") BigDecimal value,
        @NotNull Map<String, Object> metadata
) {
}
