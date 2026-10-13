package SentinellePay.Sentinelle.exception;

import lombok.Getter;


@Getter
public class InvalidFileTypeException extends RuntimeException {
	private final String providedType;
	private final String expectedType;

	public InvalidFileTypeException(String providedType, String expectedType) {
		super("Invalid file type. Expected [" + expectedType + "] but received [" + providedType + "]");
		this.providedType = providedType;
		this.expectedType = expectedType;
	}
}

