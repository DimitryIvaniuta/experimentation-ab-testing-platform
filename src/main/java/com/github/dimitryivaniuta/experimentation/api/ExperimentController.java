package com.github.dimitryivaniuta.experimentation.api;

import com.github.dimitryivaniuta.experimentation.api.dto.CreateExperimentRequest;
import com.github.dimitryivaniuta.experimentation.api.dto.ExperimentResponse;
import com.github.dimitryivaniuta.experimentation.service.ExperimentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for experiment administration and lookup.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    /**
     * Creates a new experiment.
     *
     * @param request create request
     * @return created experiment
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ExperimentResponse> create(@Valid @RequestBody final CreateExperimentRequest request) {
        return experimentService.create(request);
    }

    /**
     * Reads an experiment by key.
     *
     * @param key experiment key
     * @return experiment response
     */
    @GetMapping("/{key}")
    public Mono<ExperimentResponse> get(@PathVariable final String key) {
        return experimentService.get(key);
    }
}
