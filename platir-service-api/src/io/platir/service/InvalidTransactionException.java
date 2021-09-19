package io.platir.service;

public class InvalidTransactionException extends Exception {

	private static final long serialVersionUID = 4889760123989907636L;


	public InvalidTransactionException(String message) {
		super(message);
	}

	public InvalidTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

}
