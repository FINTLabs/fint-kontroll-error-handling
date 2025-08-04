package no.fintlabs;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

/**
 * This filter intercepts HTTP responses and adds a correlation identifier to the response headers.
 * The correlation identifier, also known as trace ID, is obtained from the active span in the tracer
 * if available. It is added to the response header with the name "X-Correlation-Id".
 * If no active span is available, the header is not added.
 * This filter ensures that outgoing HTTP responses include a trace identifier
 * that can be used for distributed tracing and correlating logs.
 * Extends the {@link HttpFilter} class to integrate servlet filtering functionality.
 *
 */
@Component
public class ResponseFilter extends HttpFilter {
    private static final String TRACE_ID_HEADER_NAME = "X-Correlation-Id";
    private final Tracer tracer;

    public ResponseFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (tracer != null && tracer.currentSpan() != null) {
            String traceId = Objects.requireNonNull(tracer.currentSpan()).context().traceId();
            response.setHeader(TRACE_ID_HEADER_NAME, traceId);
        }

        chain.doFilter(request, response);
    }
}