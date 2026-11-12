package no.fintlabs;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource(value = "classpath:META-INF/fint-kontroll-error-handling/defaults.yaml",
        factory = LibraryYamlPropertySourceFactory.class)
public class LibraryDefaultsConfiguration {
}
