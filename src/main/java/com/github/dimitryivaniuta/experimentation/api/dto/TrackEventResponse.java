package com.github.dimitryivaniuta.experimentation.api.dto;

/**
 * Response returned after a tracking event was accepted.
 *
 * @param accepted whether the event was accepted
 * @param experimentKey experiment key
 * @param variantKey assigned variant key used for aggregation
 * @param eventName tracked event name
 */
public record TrackEventResponse(boolean accepted, String experimentKey, String variantKey, String eventName) {
}
