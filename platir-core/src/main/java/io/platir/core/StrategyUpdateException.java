package io.platir.core;

public class StrategyUpdateException extends Exception {

	private static final long serialVersionUID = 249936874128090382L;

	public StrategyUpdateException(String message) {
		super(message);
	}

	public StrategyUpdateException(String message, Throwable cause) {
		super(message, cause);
	}

}
