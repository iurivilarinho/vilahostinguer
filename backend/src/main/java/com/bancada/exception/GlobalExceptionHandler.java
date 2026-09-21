package com.bancada.exception;

import com.bancada.response.ApiErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, safe(exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> dataIntegrity(DataIntegrityViolationException exception) {
        return build(HttpStatus.CONFLICT, safe(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> illegalState(IllegalStateException exception) {
        return build(HttpStatus.CONFLICT, safe(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> illegalArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, safe(exception.getMessage()));
    }

    @ExceptionHandler(DeviceConnectionException.class)
    public ResponseEntity<ApiErrorResponse> deviceConnection(DeviceConnectionException exception) {
        return build(HttpStatus.BAD_GATEWAY, safe(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(messages.isEmpty() ? List.of("Dados inválidos") : messages));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> noResource(NoResourceFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado: " + safe(exception.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> generic(Exception exception, HttpServletRequest request) {
        LOG.error("Unhandled error on {}: {}", request.getRequestURI(), exception.getMessage(), exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: " + safe(exception.getMessage()));
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(List.of(message)));
    }

    private static String safe(String message) {
        return message == null || message.isBlank() ? "Erro inesperado" : message;
    }
}
