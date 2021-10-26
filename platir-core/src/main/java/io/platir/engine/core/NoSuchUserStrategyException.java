package io.platir.engine.core;

class NoSuchUserStrategyException extends Exception {

    public NoSuchUserStrategyException(String message) {
        super(message);
    }

    public NoSuchUserStrategyException(String message, Throwable cause) {
        super(message, cause);
    }

}
