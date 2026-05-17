package com.github.dimitryivaniuta.experimentation.api;

import com.github.dimitryivaniuta.experimentation.api.dto.AssignmentResponse;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import com.github.dimitryivaniuta.experimentation.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for feature flag assignment.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/flags")
public class FlagController {

    private final AssignmentService assignmentService;

    /**
     * Returns a stable assignment and records a variant exposure event.
     *
     * @param experimentKey experiment key
     * @param userId raw user id supplied by the trusted application layer
     * @return assignment response
     */
    @GetMapping("/{experimentKey}/assignment")
    public Mono<AssignmentResponse> assign(
            @PathVariable final String experimentKey,
            @RequestHeader(name = "X-User-Id", required = false) final String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Mono.error(new BusinessRuleException("X-User-Id header is required"));
        }
        return assignmentService.assignAndExpose(experimentKey, userId);
    }
}
