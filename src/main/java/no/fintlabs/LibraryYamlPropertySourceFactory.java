package no.fintlabs;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;

public class LibraryYamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        String sourceName = name == null ? "fint-kontroll-error-handling-defaults" : name;
        return new YamlPropertySourceLoader().load(sourceName, resource.getResource()).getFirst();
    }
}
