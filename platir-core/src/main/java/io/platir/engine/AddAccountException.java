package io.platir.engine;

public class AddAccountException extends Exception {

    public AddAccountException(String message) {
        super(message);
    }

    public AddAccountException(String message, Throwable cause) {
        super(message, cause);
    }

}
