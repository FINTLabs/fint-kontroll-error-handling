package no.fintlabs.slack;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class SlackMessenger {

    @Value("${fint.kontroll.slack.api-url:https://slack.com/api/chat.postMessage}")
    private String slackApiUrl;

    @Value("${fint.kontroll.slack.bot-token:}")
    private String botToken;

    @Value("${fint.kontroll.slack.enabled:false}")
    private boolean slackEnabled;

    @Value("${fint.kontroll.authorization.authorized-org-id:}")
    private String authorizedOrgId;

    @Value("${fint.application-id:fint-kontroll-error-handling}")
    private String app;

    @Value("${fint.kontroll.slack.channel-id:}")
    private String errorChannelId;

    private final RestTemplate restTemplate;

    public SlackMessenger(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void validateChannelId() {
        if (errorChannelId == null || errorChannelId.isBlank()) {
            throw new IllegalStateException("fint.kontroll.slack.channel-id must be configured when Slack is enabled");
        }
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("fint.kontroll.slack.bot-token must be configured when Slack is enabled");
        }
    }

    public boolean sendErrorMessage(String message) {
        String label = authorizedOrgId.isBlank() ? app : authorizedOrgId + " - " + app;
        String msg = "🔺 " + label + "🔺\n\n" + message;
        return sendMessage(new SlackMessage(errorChannelId, msg));
    }

    public boolean sendMessage(SlackMessage slackMessage) {
        log.debug("Sending Slack message; enabled={}", slackEnabled);

        if (slackEnabled) {
            if (botToken.isBlank()) {
                log.warn("Slack sending is enabled but no bot token is configured");
                return false;
            }

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(botToken);
                headers.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<SlackApiResponse> response = restTemplate.exchange(
                        slackApiUrl,
                        HttpMethod.POST,
                        new HttpEntity<>(slackMessage, headers),
                        SlackApiResponse.class);

                SlackApiResponse responseBody = response.getBody();
                if (responseBody != null && responseBody.ok()) {
                    return true;
                }

                log.warn("Slack rejected the error notification: {}",
                        responseBody == null ? "empty response" : responseBody.error());
            } catch (Exception e) {
                log.warn("Unable to send Slack error notification", e);
            }
        }
        return false;
    }
}
