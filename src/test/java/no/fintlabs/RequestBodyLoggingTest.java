package no.fintlabs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestBodyLoggingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestBodyLogging requestBodyLogging = new RequestBodyLogging(objectMapper);

    @Test
    void logsACompleteBodyExactlyAtTheLimit() throws Exception {
        int emptyBodySize = objectMapper.writeValueAsBytes(Map.of("padding", "")).length;
        byte[] body = objectMapper.writeValueAsBytes(Map.of("padding", "x".repeat(8192 - emptyBodySize)));
        HttpServletRequest cached = requestBodyLogging.cacheRequest(request(body));

        assertThat(cached.getInputStream().readAllBytes()).isEqualTo(body);
        assertThat(requestBodyLogging.format(cached)).isEqualTo(new String(body, StandardCharsets.UTF_8));
    }

    @Test
    void boundsTheCacheForBodiesWithUnknownLengthWithoutTruncatingTheInput() throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(Map.of("padding", "x".repeat(16384)));
        HttpServletRequest chunked = new HttpServletRequestWrapper(request(body)) {
            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        HttpServletRequest cached = requestBodyLogging.cacheRequest(chunked);

        assertThat(cached.getInputStream().readAllBytes()).isEqualTo(body);
        assertThat(((ContentCachingRequestWrapper) cached).getContentAsByteArray()).hasSize(8193);
        assertThat(requestBodyLogging.format(cached)).isEqualTo("[omitted: body exceeds 8192 bytes]");
    }

    @Test
    void doesNotReadTheBodyToProduceALog() throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(Map.of("accessRoleId", "role-123"));
        HttpServletRequest cached = requestBodyLogging.cacheRequest(request(body));

        assertThat(requestBodyLogging.format(cached)).isEqualTo("[unavailable: body empty or not read]");
        assertThat(cached.getInputStream().readAllBytes()).isEqualTo(body);
    }

    private MockHttpServletRequest request(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body);
        return request;
    }
}
