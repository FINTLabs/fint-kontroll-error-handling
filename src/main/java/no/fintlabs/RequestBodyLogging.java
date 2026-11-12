package no.fintlabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@RequiredArgsConstructor
public class RequestBodyLogging {

    private static final int MAX_BODY_BYTES = 8192;
    private static final String OVERSIZED_BODY_ATTRIBUTE = RequestBodyLogging.class.getName() + ".oversizedBody";

    private final ObjectMapper objectMapper;

    public HttpServletRequest cacheRequest(HttpServletRequest request) {
        if (notJson(request)) {
            return request;
        }
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            request.setAttribute(OVERSIZED_BODY_ATTRIBUTE, true);
            return request;
        }
        return new ContentCachingRequestWrapper(request, MAX_BODY_BYTES + 1);
    }

    public String format(HttpServletRequest request) {
        if (notJson(request)) {
            return "[omitted: non-JSON body]";
        }
        if (Boolean.TRUE.equals(request.getAttribute(OVERSIZED_BODY_ATTRIBUTE))) {
            return "[omitted: body exceeds 8192 bytes]";
        }
        ContentCachingRequestWrapper cached = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (cached == null) {
            return "[unavailable: body empty or not read]";
        }
        byte[] body = cached.getContentAsByteArray();
        if (body.length == 0) {
            return "[unavailable: body empty or not read]";
        }
        if (body.length > MAX_BODY_BYTES) {
            return "[omitted: body exceeds 8192 bytes]";
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json == null || !json.isContainerNode()) {
                return "[omitted: JSON body is not an object or array]";
            }
            return objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT).writeValueAsString(json);
        } catch (IOException | RuntimeException ex) {
            return "[omitted: body could not be parsed]";
        }
    }

    private boolean notJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return true;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return !MediaType.APPLICATION_JSON.includes(mediaType) && !mediaType.getSubtype().endsWith("+json");
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

}
