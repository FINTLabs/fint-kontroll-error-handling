package no.fintlabs;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds the correlation id to the response header and logs.
 * Also makes the request body available for error logging.
 */
@Component
// Run after Spring Boot's observation filter and before security and application filters.
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ResponseFilter extends OncePerRequestFilter {
    private static final String TRACE_ID_HEADER_NAME = "X-Correlation-Id";
    private final Tracer tracer;
    private final RequestBodyLogging requestBodyLogging;

    public ResponseFilter(Tracer tracer, RequestBodyLogging requestBodyLogging) {
        this.tracer = tracer;
        this.requestBodyLogging = requestBodyLogging;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String correlationId = CorrelationId.getOrCreate(request, tracer);
        String previousCorrelationId = MDC.get(CorrelationId.LOGGING_CONTEXT_KEY);
        MDC.put(CorrelationId.LOGGING_CONTEXT_KEY, correlationId);
        try {
            response.setHeader(TRACE_ID_HEADER_NAME, correlationId);
            chain.doFilter(requestBodyLogging.cacheRequest(request), response);
        } finally {
            if (previousCorrelationId == null) {
                MDC.remove(CorrelationId.LOGGING_CONTEXT_KEY);
            } else {
                MDC.put(CorrelationId.LOGGING_CONTEXT_KEY, previousCorrelationId);
            }
        }
    }
}
