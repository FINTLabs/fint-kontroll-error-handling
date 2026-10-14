package no.fintlabs.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.RequestBodyLogging;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ProblemDetailFactory factory;
    private final RequestBodyLogging requestBodyLogging;

    /**
     * Handles all uncaught exceptions by creating and returning a structured problem detail response.
     * This method creates a {@link ProblemDetail} and logs the exception together with the request
     * method, path, response status, correlation ID, and the request body.
     *
     * @param ex the uncaught exception to handle
     * @param request the HTTP request during which the exception occurred
     * @return a {@link ProblemDetail} object containing structured error details
     */
    @ExceptionHandler(Throwable.class)
    public ProblemDetail handleAll(Throwable ex, HttpServletRequest request) {
        ProblemDetail problem = factory.createProblemDetail(ex, request);
        log.error("Request failed: method={}, path={}, status={}, correlationId={}, requestBody={}",
                request.getMethod(), request.getRequestURI(), problem.getStatus(),
                problem.getProperties().get("correlationId"), requestBodyLogging.format(request), ex);
        return problem;
    }
}

