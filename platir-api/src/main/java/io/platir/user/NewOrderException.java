package io.platir.user;

public class NewOrderException extends Exception {

    public NewOrderException(String message) {
        super(message);
    }

    public NewOrderException(String message, Throwable cause) {
        super(message, cause);
    }

}
