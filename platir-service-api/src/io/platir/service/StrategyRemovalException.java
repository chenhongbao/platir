package io.platir.service;

public class StrategyRemovalException extends Exception {

	private static final long serialVersionUID = 8791144592625364821L;

	public StrategyRemovalException(String message) {
		super(message);
	}

	public StrategyRemovalException(String message, Throwable cause) {
		super(message, cause);
	}

}
