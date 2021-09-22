package io.platir.service;

public class TransactionException extends Exception {

	private static final long serialVersionUID = 4889760123989907636L;


	public TransactionException(String message) {
		super(message);
	}

	public TransactionException(String message, Throwable cause) {
		super(message, cause);
	}

}
