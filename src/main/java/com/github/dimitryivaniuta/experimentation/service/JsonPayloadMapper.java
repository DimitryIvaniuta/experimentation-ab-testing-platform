package com.github.dimitryivaniuta.experimentation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts PostgreSQL JSON strings to API-safe map payloads.
 */
@Component
@RequiredArgsConstructor
public class JsonPayloadMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /**
     * Parses JSON object text into a map.
     *
     * @param json JSON object string
     * @return parsed map
     */
    public Map<String, Object> toMap(final String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            throw new BusinessRuleException("Stored JSON payload is invalid");
        }
    }
}
