package io.platir.core;

public class StrategyCreateException extends Exception {

	private static final long serialVersionUID = 6671584816175580905L;

	public StrategyCreateException(String message) {
		super(message);
	}

	public StrategyCreateException(String message, Throwable cause) {
		super(message, cause);
	}

}
