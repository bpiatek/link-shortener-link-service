package pl.bpiatek.linkshortenerlinkservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

      var validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        log.warn("Validation failed for request on [{}]: {}", request.getRequestURI(), validationErrors);

        var apiError = new ApiError(
                clock.instant(),
                "/errors/validation-error",
                "Validation Failed",
                BAD_REQUEST.value(),
                "One or more fields did not pass validation.",
                request.getRequestURI(),
                validationErrors
        );

        return new ResponseEntity<>(apiError, BAD_REQUEST);
    }

    @ExceptionHandler(ShortCodeAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleLinkAlreadyExists(
            ShortCodeAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Link shortening failed: {}", ex.getMessage());

        var apiError = new ApiError(
                clock.instant(),
                "/errors/short-url-already-exists",
                "Shortening link failed",
                CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        return new ResponseEntity<>(apiError, CONFLICT);
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ApiError> handleLinkNotFound(
            LinkNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Link not found: {}", ex.getMessage());

        var apiError = new ApiError(
                clock.instant(),
                "/errors/link-not-found",
                "Link not found",
                NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        return new ResponseEntity<>(apiError, NOT_FOUND);
    }

    @ExceptionHandler(UnableToGenerateUniqueShortUrlException.class)
    public ResponseEntity<ApiError> handleUnableToGenerateUniqueShortCode(
            UnableToGenerateUniqueShortUrlException ex, HttpServletRequest request) {

        log.error("CRITICAL: Failed to generate a unique short link. Keyspace may be exhausted.", ex);

        var apiError = new ApiError(
                clock.instant(),
                "/errors/short-url-creation-temporary-down",
                "Shortening link failed",
                SERVICE_UNAVAILABLE.value(),
                "The service is temporarily unable to generate a new link. Please try again later.",
                request.getRequestURI(),
                null
        );

        return new ResponseEntity<>(apiError, SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        var message = "Unsupported media type. Supported content types are: " + ex.getSupportedMediaTypes();

        var apiError = new ApiError(
                clock.instant(),
                "/errors/unsupported-media-type",
                "Unsupported Media Type",
                UNSUPPORTED_MEDIA_TYPE.value(),
                message,
                request.getRequestURI(),
                null
        );

        return new ResponseEntity<>(apiError, UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request: {}", ex.getMessage());

        ApiError apiError = new ApiError(
                clock.instant(),
                "/errors/malformed-request",
                "Malformed Request",
                BAD_REQUEST.value(),
                "The request body is missing or cannot be parsed.",
                request.getRequestURI(),
                null
        );

        return new ResponseEntity<>(apiError, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericExceptions(
            Exception ex, HttpServletRequest request) {

        var apiError = new ApiError(
                clock.instant(),
                "/errors/generic",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred.",
                request.getRequestURI(),
                null
        );

        log.error("Unhandled exception:", ex);

        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
