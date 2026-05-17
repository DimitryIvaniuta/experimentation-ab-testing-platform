package com.github.dimitryivaniuta.experimentation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JSON payload mapping.
 */
class JsonPayloadMapperTest {

    private final JsonPayloadMapper mapper = new JsonPayloadMapper(new ObjectMapper());

    /**
     * Verifies that variant JSON payloads are mapped to API maps.
     */
    @Test
    void shouldMapJsonToMap() {
        assertThat(mapper.toMap("{\"color\":\"green\",\"enabled\":true}"))
                .containsEntry("color", "green")
                .containsEntry("enabled", true);
    }
}
