package no.fintlabs.controller;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ProblemDetailFactory factory;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return factory.createProblemDetail(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflictException(ConflictException ex, HttpServletRequest request) {
        return factory.createProblemDetail(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex, HttpServletRequest request) {
        log.error("An unhandled exception occurred with message: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = factory.createProblemDetail(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
        problemDetail.setDetail("En uventet serverfeil oppstod");
        return problemDetail;
    }

}

