package com.github.dimitryivaniuta.experimentation.api;

import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import com.github.dimitryivaniuta.experimentation.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

/**
 * Converts service and validation exceptions into RFC 9457-style problem responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles missing resources.
     *
     * @param exception not-found exception
     * @return problem response
     */
    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleNotFound(final NotFoundException exception) {
        return Mono.just(problem(HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    /**
     * Handles business-rule violations.
     *
     * @param exception business-rule exception
     * @return problem response
     */
    @ExceptionHandler(BusinessRuleException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleBusinessRule(final BusinessRuleException exception) {
        return Mono.just(problem(HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    /**
     * Handles bean validation errors.
     *
     * @param exception validation exception
     * @return problem response
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleValidation(final WebExchangeBindException exception) {
        String detail = exception.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        return Mono.just(problem(HttpStatus.BAD_REQUEST, detail));
    }

    /**
     * Handles unexpected errors without leaking internal details.
     *
     * @param exception unexpected exception
     * @return problem response
     */
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ProblemDetail>> handleUnexpected(final Throwable exception) {
        return Mono.just(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error"));
    }

    private ResponseEntity<ProblemDetail> problem(final HttpStatus status, final String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }
}
