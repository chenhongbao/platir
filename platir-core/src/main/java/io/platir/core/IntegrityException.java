package io.platir.core;

public class IntegrityException extends Exception {

	private static final long serialVersionUID = 2304572194947377505L;

	public IntegrityException(String message) {
		super(message);
	}

	public IntegrityException(String message, Throwable cause) {
		super(message, cause);
	}
}
