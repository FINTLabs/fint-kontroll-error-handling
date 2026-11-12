package no.fintlabs.slack;

import no.fintlabs.common.HttpErrorNotificationFilter;
import no.fintlabs.LibraryDefaultsConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@AutoConfiguration
@ConditionalOnProperty(name = "fint.kontroll.slack.enabled", havingValue = "true")
@EnableConfigurationProperties(SlackAlertProperties.class)
@Import(LibraryDefaultsConfiguration.class)
public class SlackErrorNotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SlackMessenger slackMessenger() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(3000);
        return new SlackMessenger(new RestTemplate(requestFactory));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    SlackNotificationDispatcher slackNotificationDispatcher(SlackMessenger messenger) {
        return new SlackNotificationDispatcher(messenger);
    }

    @Bean
    @ConditionalOnMissingBean
    SlackErrorNotifier slackErrorNotifier(
            SlackNotificationDispatcher dispatcher,
            @Value("${fint.kontroll.slack.grafana-url}") String grafanaUrl,
            @Value("${fint.application-id}") String applicationId,
            @Value("${fint.kontroll.slack.grafana-namespace}") String namespace,
            SlackAlertProperties properties) {
        return new SlackErrorNotifier(dispatcher::dispatch, grafanaUrl, applicationId, namespace,
                properties.additionalHttpStatuses());
    }

    @Bean
    @ConditionalOnMissingBean
    HttpErrorNotificationFilter httpErrorNotificationFilter(SlackErrorNotifier notifier) {
        return new HttpErrorNotificationFilter(notifier);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "ch.qos.logback.classic.LoggerContext")
    SlackLoggingSetup slackLoggingSetup(SlackErrorNotifier notifier) {
        return new SlackLoggingSetup(notifier);
    }
}
