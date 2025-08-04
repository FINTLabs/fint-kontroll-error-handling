package no.fintlabs;

import no.fintlabs.ExceptionMappingRegistry.ExceptionDescriptor;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExceptionMappingRegistryTest {

    @Test
    public void testResolve_knownException_returnsMatchingDescriptor() {
        // Arrange
        ExceptionMappingRegistry registry = new ExceptionMappingRegistry();
        Throwable exception = new IllegalArgumentException();

        // Act
        Optional<ExceptionDescriptor> result = registry.resolve(exception);

        // Assert
        assertTrue(result.isPresent());
        ExceptionDescriptor descriptor = result.get();
        assertEquals("invalid-argument", descriptor.typeIdentifier());
        assertEquals(HttpStatus.BAD_REQUEST, descriptor.status());
    }

    @Test
    public void testResolve_inheritedException_returnsMatchingDescriptor() {
        // Arrange
        ExceptionMappingRegistry registry = new ExceptionMappingRegistry();
        Throwable exception = new AccessDeniedException("Access denied");

        // Act
        Optional<ExceptionDescriptor> result = registry.resolve(exception);

        // Assert
        assertTrue(result.isPresent());
        ExceptionDescriptor descriptor = result.get();
        assertEquals("access-denied", descriptor.typeIdentifier());
        assertEquals(HttpStatus.FORBIDDEN, descriptor.status());
    }

    @Test
    public void testResolve_unknownException_returnsEmpty() {
        // Arrange
        ExceptionMappingRegistry registry = new ExceptionMappingRegistry();
        Throwable exception = new RuntimeException("Unknown exception");

        // Act
        Optional<ExceptionDescriptor> result = registry.resolve(exception);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    public void testResolve_customRegisteredException_returnsMatchingDescriptor() {
        // Arrange
        ExceptionMappingRegistry registry = new ExceptionMappingRegistry();
        registry.getMappings().put(ConflictException.class, new ExceptionDescriptor("conflict", HttpStatus.CONFLICT));
        Throwable exception = new ConflictException("Conflict");

        // Act
        Optional<ExceptionDescriptor> result = registry.resolve(exception);

        // Assert
        assertTrue(result.isPresent());
        ExceptionDescriptor descriptor = result.get();
        assertEquals("conflict", descriptor.typeIdentifier());
        assertEquals(HttpStatus.CONFLICT, descriptor.status());
    }

    @Test
    public void testResolve_inheritedExceptionFromCustomMapping_returnsMatchingDescriptor() {
        // Arrange
        ExceptionMappingRegistry registry = new ExceptionMappingRegistry();
        registry.getMappings().put(ResourceNotFoundException.class, new ExceptionDescriptor("resource-not-found", HttpStatus.NOT_FOUND));
        Throwable exception = new ResourceNotFoundException("Resource not found");

        // Act
        Optional<ExceptionDescriptor> result = registry.resolve(exception);

        // Assert
        assertTrue(result.isPresent());
        ExceptionDescriptor descriptor = result.get();
        assertEquals("resource-not-found", descriptor.typeIdentifier());
        assertEquals(HttpStatus.NOT_FOUND, descriptor.status());
    }
}