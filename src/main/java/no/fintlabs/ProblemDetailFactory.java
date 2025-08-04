package no.fintlabs;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import no.fintlabs.exception.KontrollException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class ProblemDetailFactory {

    @Autowired(required = false)
    private Tracer tracer;

    @Value("${fint.application-id}")
    private String applicationId;

    @Autowired
    private ExceptionMappingRegistry registry;

    /**
     * Creates a {@link ProblemDetail} instance containing structured details about an exception.
     * This method generates a problem detail object based on the provided exception and the HTTP request context.
     * The object includes information such as HTTP status, exception details, correlation ID,
     * application ID, timestamp, and URI information.
     *
     * @param ex the exception for which the problem detail is being created
     * @param request the HTTP request from which contextual information is extracted
     * @return a {@link ProblemDetail} object representing the structured error detail
     */
    public ProblemDetail createProblemDetail(Throwable ex, HttpServletRequest request) {
        HttpStatus status = resolveStatus(ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());

        pd.setTitle(status.getReasonPhrase());
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setType(getTypeUri(ex));
        pd.setProperty("correlationId", getCorrelationId());
        pd.setProperty("applicationId", applicationId);
        pd.setProperty("timestamp", Instant.now().toString());

        return pd;
    }

    private URI getTypeUri(Throwable ex) {
        String baseUrl = "https://fintlabs.no/errors/";

        if (ex instanceof KontrollException pe) {
            return URI.create(baseUrl + pe.getTypeIdentifier());
        }

        return registry.resolve(ex)
                .map(desc -> URI.create(baseUrl + desc.typeIdentifier()))
                .orElse(URI.create(baseUrl + "internal-server-error"));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof KontrollException pe) {
            return pe.getStatus();
        }
        return registry.resolve(ex)
                .map(ExceptionMappingRegistry.ExceptionDescriptor::status)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getCorrelationId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return Objects.requireNonNull(tracer.currentSpan()).context().traceId();
        }
        return UUID.randomUUID().toString();
    }
}