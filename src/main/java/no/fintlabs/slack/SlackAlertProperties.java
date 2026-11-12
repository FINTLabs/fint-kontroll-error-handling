package no.fintlabs.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "fint.kontroll.slack")
public record SlackAlertProperties(Set<Integer> additionalHttpStatuses) {
    public SlackAlertProperties {
        additionalHttpStatuses = additionalHttpStatuses == null
                ? Set.of(429, 422, 409)
                : additionalHttpStatuses;
    }
}
