package com.maison.backend.config;

import com.maison.backend.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;

/**
 * Global exception handler to prevent stack traces from leaking to API clients
 * and return consistent ApiResponse error payloads.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleBadBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid request body"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Method not allowed: " + ex.getMethod()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiResponse<?>> handleDateParse(DateTimeParseException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid date/time format"));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<?>> handleNumberFormat(NumberFormatException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid numeric value"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {
        System.err.println("Unhandled exception: " + ex.getClass().getName() + " — " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An internal error occurred. Please try again."));
    }
}
