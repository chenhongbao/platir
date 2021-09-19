package io.platir.core;

public class AnnotationParsingException extends Exception {

	private static final long serialVersionUID = 1L;

	public AnnotationParsingException(String message) {
		super(message);
	}

	public AnnotationParsingException(String message, Throwable cause) {
		super(message, cause);
	}

}
