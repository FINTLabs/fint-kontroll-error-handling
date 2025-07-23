package no.fintlabs.exception;


import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends KontrollException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    @Override
    public String getTypeIdentifier() {
        return "resource-not-found";
    }
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
