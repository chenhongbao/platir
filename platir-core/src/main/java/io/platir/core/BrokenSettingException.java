package io.platir.core;

public class BrokenSettingException extends RuntimeException {

	private static final long serialVersionUID = 2115579629556863695L;

	public BrokenSettingException(String message, Throwable cause) {
		super(message, cause);
	}

	public BrokenSettingException(String message) {
		super(message);
	}

}
