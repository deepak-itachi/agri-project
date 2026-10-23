package com.agri.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.NoSuchElementException;

/** Turns exceptions into JSON the React app can show: {"message": "..."} */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException e) {
        return body(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage() == null ? "Invalid request." : e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflict(IllegalStateException e) {
        return body(HttpStatus.CONFLICT, e.getMessage() == null ? "Request conflicts with current data." : e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoSuchElementException e) {
        return body(HttpStatus.NOT_FOUND, "That record was not found.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "That file is too large. Please upload an image under 6 MB.");
    }
}
