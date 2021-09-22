package io.platir.core;

/**
 *
 * @author Chen Hongbao
 */
public class StartupException extends Exception {

    public StartupException(String message) {
        super(message);
    }

    public StartupException(String message, Throwable cause) {
        super(message, cause);
    }

}
