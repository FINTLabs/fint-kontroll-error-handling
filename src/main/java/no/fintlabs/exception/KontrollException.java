package no.fintlabs.exception;

import org.springframework.http.HttpStatus;

public abstract class KontrollException extends RuntimeException {
    protected KontrollException(String message) { super(message); }
    protected KontrollException(String message, Throwable cause) { super(message, cause); }

    public abstract String getTypeIdentifier();

    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
