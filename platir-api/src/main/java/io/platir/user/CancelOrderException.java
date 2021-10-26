package io.platir.user;

public class CancelOrderException extends Exception {

    public CancelOrderException(String message) {
        super(message);
    }

    public CancelOrderException(String message, Throwable cause) {
        super(message, cause);
    }

}
