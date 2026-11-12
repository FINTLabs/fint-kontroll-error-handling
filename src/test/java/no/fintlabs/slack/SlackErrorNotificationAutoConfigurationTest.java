package no.fintlabs.slack;

import no.fintlabs.common.HttpErrorNotificationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SlackErrorNotificationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(SlackErrorNotificationAutoConfiguration.class));

    @Test
    void remainsInactiveByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(SlackMessenger.class);
            assertThat(context).doesNotHaveBean(HttpErrorNotificationFilter.class);
        });
    }

    @Test
    void requiresChannelIdWhenEnabled() {
        contextRunner.withPropertyValues(
                "fint.kontroll.slack.enabled=true",
                "fint.kontroll.slack.bot-token=xoxb-test-token",
                "fint.kontroll.slack.grafana-url=https://grafana.example/logs")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseMessage("fint.kontroll.slack.channel-id must be configured when Slack is enabled"));
    }

    @Test
    void requiresBotTokenWhenEnabled() {
        contextRunner.withPropertyValues(
                "fint.kontroll.slack.enabled=true",
                "fint.kontroll.slack.channel-id=C12345678")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseMessage("fint.kontroll.slack.bot-token must be configured when Slack is enabled"));
    }

    @Test
    void allowsMissingGrafanaUrlWhenEnabled() {
        contextRunner.withPropertyValues(
                "fint.kontroll.slack.enabled=true",
                "fint.kontroll.slack.channel-id=C12345678",
                "fint.kontroll.slack.bot-token=xoxb-test-token")
                .run(context -> {
                    assertThat(context).hasSingleBean(SlackErrorNotifier.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackErrorNotifier.class), "grafanaUrl"))
                            .isEqualTo("");
                });
    }

    @Test
    void registersAlertingComponentsWhenEnabled() {
        contextRunner.withPropertyValues(
                "fint.kontroll.slack.enabled=true",
                "fint.kontroll.slack.channel-id=C12345678",
                "fint.kontroll.slack.bot-token=xoxb-test-token",
                "fint.kontroll.slack.grafana-url=https://grafana.example/logs",
                "fint.application-id=role-catalog",
                "POD_NAMESPACE=test-namespace")
                .run(context -> {
                    assertThat(context).hasSingleBean(SlackMessenger.class);
                    assertThat(context).hasSingleBean(SlackNotificationDispatcher.class);
                    assertThat(context).hasSingleBean(SlackErrorNotifier.class);
                    assertThat(context).hasSingleBean(HttpErrorNotificationFilter.class);
                    assertThat(context).hasSingleBean(SlackLoggingSetup.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackMessenger.class), "errorChannelId"))
                            .isEqualTo("C12345678");
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackMessenger.class), "slackApiUrl"))
                            .isEqualTo("https://slack.com/api/chat.postMessage");
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackMessenger.class), "botToken"))
                            .isEqualTo("xoxb-test-token");
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackMessenger.class), "authorizedOrgId"))
                            .isEqualTo("consumer-org");
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackMessenger.class), "app"))
                            .isEqualTo("role-catalog");
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackErrorNotifier.class), "grafanaUrl"))
                            .isEqualTo("https://grafana.example/logs");
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackErrorNotifier.class), "applicationId"))
                            .isEqualTo("role-catalog");
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlackErrorNotifier.class), "namespace"))
                            .isEqualTo("test-namespace");
                    assertThat(context.getBean(SlackErrorNotifier.class).shouldNotifyForHttpStatus(429)).isTrue();
                    assertThat(context.getBean(SlackErrorNotifier.class).shouldNotifyForHttpStatus(422)).isTrue();
                    assertThat(context.getBean(SlackErrorNotifier.class).shouldNotifyForHttpStatus(409)).isTrue();
                });
    }
}
