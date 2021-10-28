package io.platir.engine.core;

class ForceCancelException extends Exception {

    ForceCancelException(String message) {
        super(message);
    }

    ForceCancelException(String message, Throwable cause) {
        super(message, cause);
    }

}
