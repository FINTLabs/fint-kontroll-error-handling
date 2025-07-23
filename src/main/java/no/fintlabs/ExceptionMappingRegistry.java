package no.fintlabs;

import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ExceptionMappingRegistry {

    private final Map<Class<? extends Throwable>, ExceptionDescriptor> mappings = new HashMap<>();

    public ExceptionMappingRegistry() {
        register(AccessDeniedException.class, "access-denied", HttpStatus.FORBIDDEN);
        register(AuthenticationCredentialsNotFoundException.class, "missing-credentials", HttpStatus.UNAUTHORIZED);
        register(InsufficientAuthenticationException.class, "insufficient-authentication", HttpStatus.UNAUTHORIZED);
        register(IllegalArgumentException.class, "invalid-argument", HttpStatus.BAD_REQUEST);
    }

    private void register(Class<? extends Throwable> type, String identifier, HttpStatus status) {
        mappings.put(type, new ExceptionDescriptor(identifier, status));
    }

    public Optional<ExceptionDescriptor> resolve(Throwable ex) {
        return mappings.entrySet().stream()
                .filter(entry -> entry.getKey().isAssignableFrom(ex.getClass()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public record ExceptionDescriptor(String typeIdentifier, HttpStatus status) {}
}
