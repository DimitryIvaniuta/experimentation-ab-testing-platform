package com.github.dimitryivaniuta.experimentation;

import com.github.dimitryivaniuta.experimentation.config.ExperimentationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application bootstrap for the experimentation and A/B testing platform.
 */
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(ExperimentationProperties.class)
public class ExperimentationApplication {

    /**
     * Starts the reactive Spring Boot application.
     *
     * @param args command-line arguments supplied by the runtime
     */
    public static void main(final String[] args) {
        SpringApplication.run(ExperimentationApplication.class, args);
    }
}
