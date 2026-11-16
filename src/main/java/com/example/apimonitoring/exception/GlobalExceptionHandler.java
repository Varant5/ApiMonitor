package com.example.apimonitoring.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        return error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request", List.of(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        String expectedType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "a different type";

        String detail = "Parameter '%s' has invalid value '%s', expected %s"
                .formatted(ex.getName(), ex.getValue(), expectedType);

        return error(HttpStatus.BAD_REQUEST, "Invalid request", List.of(detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Malformed request body",
                List.of("Request body is missing or is not valid JSON")
        );
    }

    @ExceptionHandler(MonitorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMonitorNotFoundException(MonitorNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Monitor not found", List.of(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return error(
                HttpStatus.NOT_FOUND,
                "Not found",
                List.of("No endpoint " + ex.getHttpMethod() + " /" + ex.getResourcePath())
        );
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return error(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                List.of("Method " + ex.getMethod() + " is not supported for this endpoint")
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return error(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type",
                List.of("Content type '" + ex.getContentType() + "' is not supported, use application/json")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                List.of("An unexpected error occurred")
        );
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, List<String> errors) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), message, errors));
    }
}
