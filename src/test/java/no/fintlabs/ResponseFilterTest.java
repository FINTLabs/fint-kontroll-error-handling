package no.fintlabs;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResponseFilterTest {

    private Tracer tracer;
    private ResponseFilter filter;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        filter = new ResponseFilter(tracer, new RequestBodyLogging(new ObjectMapper()));
    }

    @AfterEach
    void cleanUp() {
        MDC.remove(CorrelationId.LOGGING_CONTEXT_KEY);
        MDC.remove("testContext");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sharesTheIdBetweenApplicationLogsHeaderAndErrorBody(boolean tracingEnabled) throws Exception {
        if (tracingEnabled) {
            Span span = mock(Span.class);
            TraceContext context = mock(TraceContext.class);
            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(context);
            when(context.traceId()).thenReturn("1234567890abcdef");
        }
        Logger logger = (Logger) LoggerFactory.getLogger(ResponseFilterTest.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start();
        logger.addAppender(appender);
        PatternLayout layout = new PatternLayout();
        layout.setContext(logger.getLoggerContext());
        layout.setPattern("%5p [correlationId=%X{correlationId:-}] %msg");
        layout.start();
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            filter.doFilter(new MockHttpServletRequest("POST", "/assignments"), response, (request, servletResponse) -> {
                logger.info("Checking permissions");
                logger.info("Creating assignment");
                var problem = new ProblemDetailFactory().createProblemDetail(
                        new ResponseStatusException(HttpStatus.CONFLICT, "Already assigned"),
                        (jakarta.servlet.http.HttpServletRequest) request);
                assertThat(problem.getProperties().get("correlationId"))
                        .isEqualTo(response.getHeader("X-Correlation-Id"));
            });
            String correlationId = response.getHeader("X-Correlation-Id");
            if (tracingEnabled) {
                assertThat(correlationId).isEqualTo("1234567890abcdef");
            } else {
                assertThat(UUID.fromString(correlationId)).isNotNull();
            }
            assertThat(appender.list).hasSize(2).allSatisfy(event -> {
                assertThat(event.getMDCPropertyMap().get(CorrelationId.LOGGING_CONTEXT_KEY)).isEqualTo(correlationId);
                assertThat(layout.doLayout(event)).contains("[correlationId=" + correlationId + "]");
            });
            assertThat(MDC.get(CorrelationId.LOGGING_CONTEXT_KEY)).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            layout.stop();
        }
    }

    @Test
    void cleansUpTheIdWhenTheFilterChainThrows() {
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> {
                    assertThat(MDC.get(CorrelationId.LOGGING_CONTEXT_KEY)).isNotBlank();
                    throw new ServletException("Request failed");
                })).isInstanceOf(ServletException.class);

        assertThat(MDC.get(CorrelationId.LOGGING_CONTEXT_KEY)).isNull();
    }

    @Test
    void restoresThePreviousIdAndPreservesOtherLoggingContext() throws Exception {
        MDC.put(CorrelationId.LOGGING_CONTEXT_KEY, "outer-context");
        MDC.put("testContext", "keep-me");

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> {
            assertThat(MDC.get(CorrelationId.LOGGING_CONTEXT_KEY)).isNotEqualTo("outer-context");
            assertThat(MDC.get("testContext")).isEqualTo("keep-me");
        });

        assertThat(MDC.get(CorrelationId.LOGGING_CONTEXT_KEY)).isEqualTo("outer-context");
        assertThat(MDC.get("testContext")).isEqualTo("keep-me");
    }

    @Test
    void assignsDifferentFallbackIdsToSequentialRequests() throws Exception {
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), first, (request, response) -> { });
        filter.doFilter(new MockHttpServletRequest(), second, (request, response) -> { });

        assertThat(first.getHeader("X-Correlation-Id")).isNotEqualTo(second.getHeader("X-Correlation-Id"));
        assertThat(MDC.get(CorrelationId.LOGGING_CONTEXT_KEY)).isNull();
    }
}
