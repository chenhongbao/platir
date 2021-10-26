package io.platir.engine.core;

class IllegalServiceStateException extends Exception {

    IllegalServiceStateException(String message) {
        super(message);
    }

    IllegalServiceStateException(String message, Throwable cause) {
        super(message, cause);
    }

}
