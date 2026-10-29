package com.example.meetingscheduler.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import com.example.meetingscheduler.ai.AiErrors;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MeetingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(MeetingNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), List.of());
    }

    @ExceptionHandler(SchedulingConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(SchedulingConflictException ex) {
        List<String> details = ex.getConflicts().stream()
                .map(c -> c.meetingId() + " (" + c.topic() + ") conflicts for " + c.conflictingParticipantEmails())
                .toList();
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), details);
    }

    @ExceptionHandler({InvalidMeetingTimeException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), List.of());
    }

    @ExceptionHandler(AiProcessingException.class)
    public ResponseEntity<ErrorResponse> handleAiFailure(AiProcessingException ex) {
        log.warn("AI processing failed: {}", ex.getMessage(), ex);
        List<String> details = AiErrors.detailChain(ex);
        return build(HttpStatus.BAD_GATEWAY, "AI Processing Error", ex.getMessage(), details);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", "Request validation failed.", details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = "Required query parameter '" + ex.getParameterName() + "' is missing.";
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message, List.of(ex.getParameterType()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter '" + ex.getName() + "'.";
        return build(HttpStatus.BAD_REQUEST, "Bad Request", message, List.of(String.valueOf(ex.getValue())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Malformed JSON or invalid date/time format. Use ISO-8601 instants, e.g. 2026-11-10T09:00:00Z.",
                List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException ex) {
        log.error("Database error", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Database Error", "A database error occurred.", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String supported = ex.getSupportedHttpMethods() != null
                ? ex.getSupportedHttpMethods().toString()
                : "unknown";
        String message = "HTTP method " + ex.getMethod() + " is not supported for this URL. Supported methods: " + supported;
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", message, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.", List.of());
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, List<String> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), error, message, details));
    }
}
