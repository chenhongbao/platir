package io.platir.engine.core;

class NoSuchOrderException extends Exception {

    NoSuchOrderException(String message) {
        super(message);
    }

    NoSuchOrderException(String message, Throwable cause) {
        super(message, cause);
    }

}
