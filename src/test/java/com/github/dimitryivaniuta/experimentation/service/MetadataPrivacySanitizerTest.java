package com.github.dimitryivaniuta.experimentation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for event metadata privacy sanitization.
 */
class MetadataPrivacySanitizerTest {

    private final MetadataPrivacySanitizer sanitizer = new MetadataPrivacySanitizer();

    /**
     * Verifies that obvious PII-like metadata keys are redacted while safe dimensions remain available.
     */
    @Test
    void shouldRedactSensitiveMetadataKeys() {
        Map<String, Object> result = sanitizer.sanitize(Map.of(
                "email", "john@example.com",
                "currency", "EUR",
                "device", Map.of("ipAddress", "127.0.0.1", "family", "desktop")
        ));

        assertThat(result).containsEntry("email", "[REDACTED]");
        assertThat(result).containsEntry("currency", "EUR");
        assertThat((Map<?, ?>) result.get("device"))
                .containsEntry("ipAddress", "[REDACTED]")
                .containsEntry("family", "desktop");
    }
}
