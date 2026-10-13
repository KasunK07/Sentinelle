package SentinellePay.Sentinelle.exception;

import lombok.Getter;


@Getter
public class FileParseException extends RuntimeException {
	private final String fileName;

	public FileParseException(String fileName, String message, Throwable cause) {
		super("Failed to parse file [" + fileName + "]: " + message, cause);
		this.fileName = fileName;
	}
}

