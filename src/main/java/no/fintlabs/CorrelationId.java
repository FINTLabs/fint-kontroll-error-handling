package no.fintlabs;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Keeps one correlation id for a request.
 * The same id is used in logs, the response header and the problem detail body.
 */
public final class CorrelationId {

    public static final String LOGGING_CONTEXT_KEY = "correlationId";
    private static final String REQUEST_ATTRIBUTE = CorrelationId.class.getName();

    private CorrelationId() {
    }

    public static String getOrCreate(HttpServletRequest request, Tracer tracer) {
        if (request.getAttribute(REQUEST_ATTRIBUTE) instanceof String correlationId) {
            return correlationId;
        }
        Span span = tracer != null ? tracer.currentSpan() : null;
        String correlationId = span != null ? span.context().traceId() : UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        return correlationId;
    }
}
