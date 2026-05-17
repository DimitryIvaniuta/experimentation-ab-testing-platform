package com.github.dimitryivaniuta.experimentation.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates Kafka topics required by the platform when the broker allows auto topic administration.
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Creates the exposure event topic.
     *
     * @param properties application properties containing topic names
     * @return Kafka topic definition for exposures
     */
    @Bean
    public NewTopic exposureTopic(final ExperimentationProperties properties) {
        return TopicBuilder.name(properties.kafka().exposureTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Creates the tracking event topic.
     *
     * @param properties application properties containing topic names
     * @return Kafka topic definition for tracking events
     */
    @Bean
    public NewTopic trackingTopic(final ExperimentationProperties properties) {
        return TopicBuilder.name(properties.kafka().trackingTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
