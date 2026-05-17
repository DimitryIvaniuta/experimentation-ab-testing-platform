package com.github.dimitryivaniuta.experimentation.exception;

/**
 * Raised when a request is syntactically valid but violates an application rule.
 */
public class BusinessRuleException extends RuntimeException {

    /**
     * Creates a business-rule exception.
     *
     * @param message human-readable error message
     */
    public BusinessRuleException(final String message) {
        super(message);
    }
}
