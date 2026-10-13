package SentinellePay.Sentinelle.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BinMasterNotLoadedException.class)
    public ResponseEntity<ApiErrorResponse> handleBinNotLoaded(
            BinMasterNotLoadedException ex, HttpServletRequest req) {
        log.warn("BIN master not loaded — {}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidFileType(
            InvalidFileTypeException ex, HttpServletRequest req) {
        log.warn("Invalid file type at {} — {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiErrorResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(FileParseException.class)
    public ResponseEntity<ApiErrorResponse> handleFileParse(
            FileParseException ex, HttpServletRequest req) {
        log.warn("File parse error at {} — {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            ValidationException ex, HttpServletRequest req) {
        log.warn("Validation error at {} — {}", req.getRequestURI(), ex.getErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message("Validation failed")
                        .errors(ex.getErrors())
                        .path(req.getRequestURI())
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleFileSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        log.warn("File too large at {}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiErrorResponse.of(HttpStatus.PAYLOAD_TOO_LARGE,
                        "Uploaded file exceeds the maximum allowed size of 50MB.", req.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException ex, HttpServletRequest req) {
        log.warn("Missing request part '{}' at {}", ex.getRequestPartName(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST,
                        "Required request part '" + ex.getRequestPartName() + "' is missing.",
                        req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred. Please contact support.", req.getRequestURI()));
    }
}