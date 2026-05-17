package com.github.dimitryivaniuta.experimentation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Redis configuration for privacy-safe assignment caching.
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a String-to-String reactive Redis template used for JSON assignment cache values.
     *
     * @param factory reactive Redis connection factory provided by Spring Boot
     * @return string Redis template
     */
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(final ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(factory, RedisSerializationContext.string());
    }
}
