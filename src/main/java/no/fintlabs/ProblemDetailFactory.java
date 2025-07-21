package no.fintlabs;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class ProblemDetailFactory {

    @Value("${fint.application-id:error-handling}")
    private String applicationId;

    @Autowired(required = false)
    private Tracer tracer;

    public ProblemDetail createProblemDetail(Throwable ex, HttpStatus status, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setProperty("correlationId", getCorrelationId());
        pd.setProperty("applicationId", applicationId);
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    private String getCorrelationId() {
        return (tracer != null && tracer.currentSpan() != null)
                ? Objects.requireNonNull(tracer.currentSpan()).context().traceId()
                : UUID.randomUUID().toString();
    }
}