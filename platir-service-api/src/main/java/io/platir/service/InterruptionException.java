package io.platir.service;

public class InterruptionException extends Exception {

	private static final long serialVersionUID = -7161898744343540170L;

	public InterruptionException(String message) {
		super(message);
	}

	public InterruptionException(String message, Throwable cause) {
		super(message, cause);
	}
}
