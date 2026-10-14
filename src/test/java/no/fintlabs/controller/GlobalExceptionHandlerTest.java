package no.fintlabs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import no.fintlabs.CorrelationId;
import no.fintlabs.ExceptionMappingRegistry;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.RequestBodyLogging;
import no.fintlabs.ResponseFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private Tracer tracer;

    @BeforeEach
    void setUp() {
        tracer = mock(Tracer.class);
        ProblemDetailFactory factory = new ProblemDetailFactory();
        ReflectionTestUtils.setField(factory, "registry", new ExceptionMappingRegistry());
        ReflectionTestUtils.setField(factory, "applicationId", "role-catalog");
        ReflectionTestUtils.setField(factory, "tracer", tracer);
        RequestBodyLogging requestBodyLogging = new RequestBodyLogging(new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(new ResponseFilter(tracer, requestBodyLogging))
                .setControllerAdvice(new GlobalExceptionHandler(factory, requestBodyLogging))
                .build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesConflictStatusAndLogsRequest(boolean tracingEnabled, CapturedOutput output) throws Exception {
        if (tracingEnabled) {
            Span span = mock(Span.class);
            TraceContext context = mock(TraceContext.class);
            when(tracer.currentSpan()).thenReturn(span);
            when(span.context()).thenReturn(context);
            when(context.traceId()).thenReturn("1234567890abcdef");
        }

        String body = """
                {
                  "accessRoleId": "role-123",
                  "userId": "user-42",
                  "scopeId": 7,
                  "orgUnitIds": ["org-123", "org-456"],
                  "includeSubOrgUnits": true
                }
                """;
        MvcResult result = mockMvc.perform(post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("User already has assignments for this role."))
                .andExpect(jsonPath("$.instance").value("/assignments"))
                .andExpect(jsonPath("$.applicationId").value("role-catalog"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andReturn();

        String correlationId = new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("correlationId").asText();
        assertThat(result.getResponse().getHeader("X-Correlation-Id")).isEqualTo(correlationId);
        assertThat(MDC.get(CorrelationId.LOGGING_CONTEXT_KEY)).isNull();
        if (tracingEnabled) {
            assertThat(correlationId).isEqualTo("1234567890abcdef");
        }
        assertThat(output.getAll()).contains(
                "Request failed: method=POST, path=/assignments, status=409, correlationId=" + correlationId);
        assertThat(output.getAll()).contains(
                "org.springframework.web.server.ResponseStatusException: 409 CONFLICT");
        assertThat(output.getAll()).contains(
                "requestBody={\"accessRoleId\":\"role-123\",\"userId\":\"user-42\","
                        + "\"scopeId\":7,\"orgUnitIds\":[\"org-123\",\"org-456\"],\"includeSubOrgUnits\":true}");
    }

    @Test
    void logsAnUnavailableBodyWithoutChangingTheErrorResponse(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/assignments").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());

        assertThat(output.getAll()).contains("requestBody=[unavailable: body empty or not read]");
    }

    @Test
    void omitsLargeBodiesWithoutTruncatingTheControllersInput(CapturedOutput output) throws Exception {
        String body = new ObjectMapper().writeValueAsString(Map.of("padding", "x".repeat(8192)));
        mockMvc.perform(post("/assignments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        assertThat(output.getAll()).contains("requestBody=[omitted: body exceeds 8192 bytes]");
        assertThat(output.getAll()).doesNotContain("x".repeat(100));
    }

    @Test
    void omitsNonJsonBodies(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/assignments").contentType(MediaType.TEXT_PLAIN).content("private-plain-text"))
                .andExpect(status().isUnsupportedMediaType());

        assertThat(output.getAll()).contains("requestBody=[omitted: non-JSON body]");
        assertThat(output.getAll()).doesNotContain("private-plain-text");
    }

    @Test
    void successfulRequestBodiesRemainReadableAndAreNotLogged(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/successful-assignments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessRoleId\":\"successful-role-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessRoleId").value("successful-role-123"));

        assertThat(output.getAll()).doesNotContain("requestBody=", "successful-role-123");
    }

    @RestController
    static class TestController {

        @PostMapping("/assignments")
        public void assignRole(@RequestBody(required = false) Map<String, Object> body) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has assignments for this role.");
        }

        @PostMapping("/successful-assignments")
        public Map<String, Object> successfulAssignment(@RequestBody Map<String, Object> body) {
            return body;
        }
    }
}
