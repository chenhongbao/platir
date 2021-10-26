package io.platir.engine.core;

public class InsufficientInfoException extends Exception {

    public InsufficientInfoException(String message) {
        super(message);
    }

    public InsufficientInfoException(String message, Throwable cause) {
        super(message, cause);
    }

}
