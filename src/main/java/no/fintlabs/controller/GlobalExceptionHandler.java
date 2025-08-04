package no.fintlabs.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.ProblemDetailFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ProblemDetailFactory factory;

    /**
     * Handles all uncaught exceptions by creating and returning a structured problem detail response.
     * This method logs the exception and constructs a {@link ProblemDetail} using the exception details
     * and the HTTP request context.
     *
     * @param ex the uncaught exception to handle
     * @param request the HTTP request during which the exception occurred
     * @return a {@link ProblemDetail} object containing structured error details
     */
    @ExceptionHandler(Throwable.class)
    public ProblemDetail handleAll(Throwable ex, HttpServletRequest request) {
        logError(ex);
        return factory.createProblemDetail(ex, request);
    }

    private void logError(Throwable ex) {
        log.error("Unhandled exception", ex);
    }
}

