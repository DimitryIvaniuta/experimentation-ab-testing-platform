package com.github.dimitryivaniuta.experimentation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.experimentation.config.ExperimentationProperties;
import com.github.dimitryivaniuta.experimentation.domain.AssignmentRecord;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Redis-backed cache for stable assignments keyed only by experiment key and user hash.
 */
@Component
@RequiredArgsConstructor
public class AssignmentCache {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ExperimentationProperties properties;

    /**
     * Reads one assignment from Redis.
     *
     * @param experimentKey experiment key
     * @param userHash privacy-safe user hash
     * @return assignment when found and deserializable
     */
    public Mono<AssignmentRecord> get(final String experimentKey, final String userHash) {
        return redisTemplate.opsForValue()
                .get(key(experimentKey, userHash))
                .flatMap(json -> {
                    try {
                        CachedAssignment cached = objectMapper.readValue(json, CachedAssignment.class);
                        return Mono.just(new AssignmentRecord(
                                cached.id(), cached.experimentKey(), cached.userHash(), cached.variantKey(), cached.assignedAt()
                        ));
                    } catch (Exception exception) {
                        return Mono.empty();
                    }
                });
    }

    /**
     * Stores one assignment in Redis with configured TTL.
     *
     * @param record assignment to cache
     * @return cached assignment
     */
    public Mono<AssignmentRecord> put(final AssignmentRecord record) {
        try {
            CachedAssignment cached = new CachedAssignment(record.id(), record.experimentKey(), record.userHash(), record.variantKey(), record.assignedAt());
            String json = objectMapper.writeValueAsString(cached);
            return redisTemplate.opsForValue()
                    .set(key(record.experimentKey(), record.userHash()), json, properties.assignmentCache().ttl())
                    .thenReturn(record);
        } catch (JsonProcessingException exception) {
            return Mono.just(record);
        }
    }

    private String key(final String experimentKey, final String userHash) {
        return "ab:assignment:" + experimentKey + ':' + userHash;
    }

    /**
     * Serializable cache representation.
     *
     * @param id assignment id
     * @param experimentKey experiment key
     * @param userHash user hash
     * @param variantKey variant key
     * @param assignedAt assignment timestamp
     */
    public record CachedAssignment(UUID id, String experimentKey, String userHash, String variantKey, Instant assignedAt) {
    }
}
