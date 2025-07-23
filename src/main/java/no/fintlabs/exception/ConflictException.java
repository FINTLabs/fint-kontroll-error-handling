package no.fintlabs.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends KontrollException {
    public ConflictException(String message) {
        super(message);
    }

    @Override
    public String getTypeIdentifier() {
        return "conflict";
    }
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
