package io.platir.engine;

public class RemoveUserException extends Exception {

    public RemoveUserException(String message) {
        super(message);
    }

    public RemoveUserException(String message, Throwable cause) {
        super(message, cause);
    }

}
