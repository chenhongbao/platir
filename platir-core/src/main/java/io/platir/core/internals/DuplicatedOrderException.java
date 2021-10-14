package io.platir.core.internals;

/**
 *
 * @author Chen Hongbao
 */
public class DuplicatedOrderException extends Exception {

    public DuplicatedOrderException(String message) {
        super(message);
    }

    public DuplicatedOrderException(String message, Throwable cause) {
        super(message, cause);
    }

}
