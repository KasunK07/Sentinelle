package SentinellePay.Sentinelle.exception;

import java.util.List;

import lombok.Getter;


@Getter
public class ValidationException extends RuntimeException {
	private final List<String> errors;

	public ValidationException(List<String> errors) {
		super("Validation failed");
		this.errors = List.copyOf(errors);
	}

	public ValidationException(String singleError) {
		this(List.of(singleError));
	}
}

