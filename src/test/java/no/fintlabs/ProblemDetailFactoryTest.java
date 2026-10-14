package no.fintlabs;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.KontrollException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

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

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 503, 599})
    public void testCreateProblemDetail_withResponseStatusException(int status) {
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatusCode.valueOf(status), "User already has assignments for this role.");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/assignments");

        ProblemDetail problemDetail = problemDetailFactory.createProblemDetail(exception, request);

        assertThat(problemDetail.getStatus()).isEqualTo(status);
        assertThat(problemDetail.getDetail()).isEqualTo("User already has assignments for this role.");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("about:blank"));
        assertThat(problemDetail.getInstance()).isEqualTo(URI.create("/test/assignments"));
        assertThat(problemDetail.getProperties().get("correlationId")).isNotNull();
        assertThat(problemDetail.getProperties().get("timestamp")).isNotNull();
    }

    @Test
    public void testCreateProblemDetail_preservesSpringProblemDetails() {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid role");
        body.setTitle("Role validation failed");
        body.setType(URI.create("https://fintlabs.no/errors/invalid-role"));
        body.setProperty("roleId", "role-123");
        ErrorResponseException exception = new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY, body, null);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/roles");

        ProblemDetail problemDetail = problemDetailFactory.createProblemDetail(exception, request);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Invalid role");
        assertThat(problemDetail.getTitle()).isEqualTo("Role validation failed");
        assertThat(problemDetail.getType()).isEqualTo(URI.create("https://fintlabs.no/errors/invalid-role"));
        assertThat(problemDetail.getProperties().get("roleId")).isEqualTo("role-123");
        assertThat(problemDetail.getProperties().get("correlationId")).isNotNull();
    }

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
