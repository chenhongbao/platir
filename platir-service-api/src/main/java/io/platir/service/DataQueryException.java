package io.platir.service;

/**
 *
 * @author Chen Hongbao
 */
public class DataQueryException extends Exception {

    public DataQueryException(String message) {
        super(message);
    }

    public DataQueryException(String message, Throwable cause) {
        super(message, cause);
    }

}
