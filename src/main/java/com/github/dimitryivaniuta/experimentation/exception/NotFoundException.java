package com.github.dimitryivaniuta.experimentation.exception;

/**
 * Raised when a requested resource does not exist.
 */
public class NotFoundException extends RuntimeException {

    /**
     * Creates a not-found exception.
     *
     * @param message human-readable error message
     */
    public NotFoundException(final String message) {
        super(message);
    }
}
