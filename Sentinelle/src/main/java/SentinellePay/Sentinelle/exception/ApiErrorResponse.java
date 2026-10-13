package SentinellePay.Sentinelle.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/**
 * Standardized error response returned by the REST API.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
	private final int status;
	private final String error;
	private final String message;
	private final List<String> errors;
	private final String path;
	private final String timestamp;
	private final String traceId;

	/**
	 * Convenience factory for single-message error responses.
	 *
	 * @param httpStatus the HTTP status to use
	 * @param message the human-readable error message
	 * @param path the request URI path
	 * @return a populated {@link ApiErrorResponse}
	 */
	public static ApiErrorResponse of(HttpStatus httpStatus, String message, String path) {
		return ApiErrorResponse.builder()
				.status(httpStatus.value())
				.error(httpStatus.getReasonPhrase())
				.message(message)
				.path(path)
				.timestamp(Instant.now().toString())
				.build();
	}
}
