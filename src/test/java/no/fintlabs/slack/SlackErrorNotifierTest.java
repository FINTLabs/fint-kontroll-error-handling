package no.fintlabs.slack;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SlackErrorNotifierTest {

    @Test
    void shouldSendHttpAndLogAlertsWithoutGrafanaLineWhenUrlIsBlank() {
        SlackMessenger slackMessenger = mock(SlackMessenger.class);
        SlackErrorNotifier notifier = new SlackErrorNotifier(slackMessenger,
                "", "role-catalog", "test-namespace", Set.of(429));
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.ERROR);
        event.setLoggerName("no.fintlabs.Example");
        event.setMessage("Failure");
        event.setMDCPropertyMap(Map.of());

        notifier.notifyHttpError(500, new MockHttpServletRequest("GET", "/example"), null);
        notifier.notifyErrorLog(event);

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(slackMessenger, times(2)).sendErrorMessage(messages.capture());
        assertThat(messages.getAllValues()).allSatisfy(message ->
                assertThat(message).doesNotContain("Investigate", "Open Grafana"));
        assertThat(messages.getAllValues().get(0)).contains("HTTP error response", "GET /example");
        assertThat(messages.getAllValues().get(1)).contains("Error log detected", "Failure");
    }

    @Test
    void shouldLinkToThirtySecondsAroundErrorLogWithoutCorrelationSearch() {
        SlackMessenger slackMessenger = mock(SlackMessenger.class);
        SlackErrorNotifier notifier = new SlackErrorNotifier(slackMessenger,
                "https://grafana.example/logs?from=now-12h&to=now&timezone=browser&orgId=1&tab=queries&var-filters=old&var-lineContains=old",
                "fint-kontroll-assignment-catalog", "fintlabs-no", Set.of(429));
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.ERROR);
        event.setLoggerName("no.fintlabs.Example");
        event.setMessage("Failure");
        event.setTimeStamp(1_700_000_000_000L);
        event.setMDCPropertyMap(Map.of("correlationId", "trace-123"));

        notifier.notifyErrorLog(event);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(slackMessenger).sendErrorMessage(message.capture());
        assertThat(message.getValue())
                .contains("/logs?timezone=browser&orgId=1&tab=queries&from=1699999970000&to=1700000030000")
                .contains("var-filters=app%7C%3D%7Cfint-kontroll-assignment-catalog")
                .contains("var-filters=namespace%7C%3D%7Cfintlabs-no")
                .contains("Correlation ID:* `trace-123`")
                .doesNotContain("var-lineContains=")
                .doesNotContain("now-12h", "var-filters=old", "var-lineContains=old");
    }

    @Test
    void shouldLeaveSearchUnsetForAnUncorrelatedLogEvent() {
        SlackMessenger slackMessenger = mock(SlackMessenger.class);
        SlackErrorNotifier notifier = new SlackErrorNotifier(slackMessenger,
                "https://grafana.example/logs?var-lineContains=old",
                "fint-kontroll-assignment-catalog", "fintlabs-no", Set.of(429));
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.ERROR);
        event.setLoggerName("no.fintlabs.Example");
        event.setMessage("Failure");
        event.setTimeStamp(1_700_000_000_000L);
        event.setMDCPropertyMap(Map.of());

        notifier.notifyErrorLog(event);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(slackMessenger).sendErrorMessage(message.capture());
        assertThat(message.getValue())
                .contains("from=1699999970000&to=1700000030000")
                .doesNotContain("var-lineContains=");
    }
}
