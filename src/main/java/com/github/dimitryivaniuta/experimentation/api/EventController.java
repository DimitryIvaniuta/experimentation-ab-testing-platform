package com.github.dimitryivaniuta.experimentation.api;

import com.github.dimitryivaniuta.experimentation.api.dto.TrackEventRequest;
import com.github.dimitryivaniuta.experimentation.api.dto.TrackEventResponse;
import com.github.dimitryivaniuta.experimentation.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for custom event tracking.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final TrackingService trackingService;

    /**
     * Accepts and aggregates one tracking event.
     *
     * @param request tracking event request
     * @return accepted event response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<TrackEventResponse> track(@Valid @RequestBody final TrackEventRequest request) {
        return trackingService.track(request);
    }
}
