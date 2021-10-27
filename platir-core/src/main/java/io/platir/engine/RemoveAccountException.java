package io.platir.engine;

public class RemoveAccountException extends Exception {

    public RemoveAccountException(String message) {
        super(message);
    }

    public RemoveAccountException(String message, Throwable cause) {
        super(message, cause);
    }

}
