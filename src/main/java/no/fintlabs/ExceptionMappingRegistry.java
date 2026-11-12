package no.fintlabs;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.BadJwtException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ExceptionMappingRegistry {

    @Getter
    private final Map<Class<? extends Throwable>, ExceptionDescriptor> mappings = new HashMap<>();

    public ExceptionMappingRegistry() {
        register(AccessDeniedException.class, "access-denied", HttpStatus.FORBIDDEN);
        register(AuthenticationCredentialsNotFoundException.class, "missing-credentials", HttpStatus.UNAUTHORIZED);
        register(BadJwtException.class, "bad-jwt-token", HttpStatus.UNAUTHORIZED);
        register(AuthenticationException.class, "authentication-exception", HttpStatus.UNAUTHORIZED);
        register(InsufficientAuthenticationException.class, "insufficient-authentication", HttpStatus.UNAUTHORIZED);
        register(IllegalArgumentException.class, "invalid-argument", HttpStatus.BAD_REQUEST);
    }

    private void register(Class<? extends Throwable> type, String identifier, HttpStatus status) {
        mappings.put(type, new ExceptionDescriptor(identifier, status));
    }

    /**
     * Resolves the provided {@link Throwable} to an {@link ExceptionDescriptor} if a mapping exists.
     * The resolution is based on the class of the exception or any of its assignable superclasses
     * registered in the mappings.
     *
     * @param ex the exception to resolve
     * @return an {@link Optional} containing the {@link ExceptionDescriptor} if a mapping exists,
     *         or an empty {@link Optional} if no mapping is found
     */
    public Optional<ExceptionDescriptor> resolve(Throwable ex) {
        for (Class<?> type = ex.getClass(); type != null; type = type.getSuperclass()) {
            ExceptionDescriptor descriptor = mappings.get(type);
            if (descriptor != null) {
                return Optional.of(descriptor);
            }
        }
        return Optional.empty();
    }

    public record ExceptionDescriptor(String typeIdentifier, HttpStatus status) {}
}
