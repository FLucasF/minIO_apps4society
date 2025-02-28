package com.apps4society.MinIO_API.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 400 BAD REQUEST
    @ExceptionHandler({InvalidInputException.class, InvalidFileException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 403 FORBIDDEN
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // 404 NOT FOUND
    @ExceptionHandler({MediaNotFoundException.class, BucketNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // 409 CONFLICT – Adicionado para DuplicateFileException
    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateFile(DuplicateFileException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // 405 METHOD NOT ALLOWED
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    // 406 NOT ACCEPTABLE
    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(UnsupportedMediaTypeException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, ex.getMessage(), request);
    }

    // 413 PAYLOAD TOO LARGE
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handlePayloadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo enviado excede o limite permitido.", request);
    }

    // 502 BAD GATEWAY
    @ExceptionHandler({MinIOConnectionException.class, ExternalServiceException.class})
    public ResponseEntity<ErrorResponse> handleBadGateway(RuntimeException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
    }

    // 503 SERVICE UNAVAILABLE
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(FileStorageException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    // 500 INTERNAL SERVER ERROR – pega tudo que não foi tratado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado.", request);
    }

    // Método genérico para construir respostas de erro
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(status, message, request.getRequestURI());
        return ResponseEntity.status(status).body(errorResponse);
    }
}
