package no.fintlabs.slack;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Marks log events that have their own, richer Slack notification. The log
 * appender must not turn these into a second notification.
 */
public final class SlackNotificationMarkers {

    public static final Marker HTTP_ERROR_NOTIFICATION_LOG = MarkerFactory.getMarker("SLACK_HTTP_ERROR_NOTIFICATION_LOG");

    private SlackNotificationMarkers() {
    }
}
