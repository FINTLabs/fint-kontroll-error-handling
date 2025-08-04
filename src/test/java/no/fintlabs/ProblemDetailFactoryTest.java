package no.fintlabs;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.KontrollException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class ProblemDetailFactoryTest {

    @Value("${fint.application-id}")
    private String applicationId;

    @Mock
    private Tracer tracer;

    @Mock
    private ExceptionMappingRegistry registry;

    @InjectMocks
    private ProblemDetailFactory problemDetailFactory;

    @Test
    public void testCreateProblemDetail_withKontrollException() {
        // Arrange
        KontrollException exception = new ConflictException("Custom exception");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/path");

        // Act
        ProblemDetail problemDetail = problemDetailFactory.createProblemDetail(exception, request);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Custom exception");
        assertThat(problemDetail.getTitle()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
        assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/test/path"));
        assertThat(problemDetail.getType().toString()).contains("conflict");
        assertThat(problemDetail.getProperties().get("applicationId")).isEqualTo(applicationId);
        assertThat(problemDetail.getProperties().get("timestamp")).isNotNull();
        assertThat(problemDetail.getProperties().get("correlationId")).isNotNull();
    }

    @Test
    public void testCreateProblemDetail_withUnhandledException() {
        // Arrange
        RuntimeException exception = new RuntimeException("Unhandled exception");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/unhandled");
        when(registry.resolve(Mockito.any())).thenReturn(Optional.empty());

        // Act
        ProblemDetail problemDetail = problemDetailFactory.createProblemDetail(exception, request);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Unhandled exception");
        assertThat(problemDetail.getTitle()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/test/unhandled"));
        assertThat(problemDetail.getType().toString()).contains("internal-server-error");
        assertThat(problemDetail.getProperties().get("applicationId")).isEqualTo(applicationId);
        assertThat(problemDetail.getProperties().get("timestamp")).isNotNull();
        assertThat(problemDetail.getProperties().get("correlationId")).isNotNull();
    }

    @Test
    public void testCreateProblemDetail_withCorrelationIdFromTracer() {
        // Arrange
        RuntimeException exception = new RuntimeException("Test exception");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/correlation");
        when(registry.resolve(Mockito.any())).thenReturn(Optional.empty());

        Span span = mock(Span.class);
        when(tracer.currentSpan()).thenReturn(span);
        TraceContext context = mock(TraceContext.class);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("1234567890abcdef");

        // Act
        ProblemDetail problemDetail = problemDetailFactory.createProblemDetail(exception, request);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Test exception");
        assertThat(problemDetail.getTitle()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/test/correlation"));
        assertThat(problemDetail.getType().toString()).contains("internal-server-error");
        assertThat(problemDetail.getProperties().get("applicationId")).isEqualTo(applicationId);
        assertThat(problemDetail.getProperties().get("timestamp")).isNotNull();
        assertThat(problemDetail.getProperties().get("correlationId")).isEqualTo("1234567890abcdef");
    }

    @Test
    public void testCreateProblemDetail_generatesDefaultCorrelationId() {
        // Arrange
        RuntimeException exception = new RuntimeException("Test exception without tracer");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/default-correlation");

        when(registry.resolve(Mockito.any())).thenReturn(Optional.empty());

        // Act
        ProblemDetail problemDetail = problemDetailFactory.createProblemDetail(exception, request);

        // Assert
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Test exception without tracer");
        assertThat(problemDetail.getTitle()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/test/default-correlation"));
        assertThat(problemDetail.getType().toString()).contains("internal-server-error");
        assertThat(problemDetail.getProperties().get("applicationId")).isEqualTo(applicationId);
        assertThat(problemDetail.getProperties().get("timestamp")).isNotNull();
        assertThat(problemDetail.getProperties().get("correlationId")).isNotNull();
    }
}