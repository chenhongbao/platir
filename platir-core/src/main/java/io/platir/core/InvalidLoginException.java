package io.platir.core;

public class InvalidLoginException extends Exception {

	private static final long serialVersionUID = 4246842583562063176L;

	public InvalidLoginException(String message) {
		super(message);
	}

	public InvalidLoginException(String message, Throwable cause) {
		super(message, cause);
	}

}
