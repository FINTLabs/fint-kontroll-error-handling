package no.fintlabs.slack;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import jakarta.annotation.PreDestroy;

/** Connects Slack alerts to Logback while the application is running. */
public class SlackLoggingSetup {

    private final SlackErrorNotifier slackErrorNotifier;

    public SlackLoggingSetup(SlackErrorNotifier slackErrorNotifier) {
        this.slackErrorNotifier = slackErrorNotifier;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connectSlackAlertsToLogging() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext)) {
            return;
        }

        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender(SlackErrorLogAppender.NAME) != null) {
            return;
        }

        SlackErrorLogAppender appender = new SlackErrorLogAppender(slackErrorNotifier);
        appender.setContext(loggerContext);
        appender.start();
        rootLogger.addAppender(appender);
    }

    @PreDestroy
    public void disconnectSlackAlertsFromLogging() {
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
            Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            if (rootLogger.getAppender(SlackErrorLogAppender.NAME) instanceof SlackErrorLogAppender appender
                    && appender.getNotifier() == slackErrorNotifier) {
                rootLogger.detachAppender(appender);
                appender.stop();
            }
        }
    }
}
