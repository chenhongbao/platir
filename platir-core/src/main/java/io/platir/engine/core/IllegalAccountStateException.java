package io.platir.engine.core;

public class IllegalAccountStateException extends Exception {

    public IllegalAccountStateException(String message) {
        super(message);
    }

    public IllegalAccountStateException(String message, Throwable cause) {
        super(message, cause);
    }

}
