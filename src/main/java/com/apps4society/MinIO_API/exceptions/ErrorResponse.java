package com.apps4society.MinIO_API.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {
    private final int status;
    private final String error; // Nome do erro (ex: "Bad Request")
    private final String message; // Detalhes do erro
    private final String path; // Endpoint que gerou o erro
    private final LocalDateTime timestamp;

    public ErrorResponse(HttpStatus status, String message, String path) {
        this.status = status.value();
        this.error = status.getReasonPhrase(); // Exemplo: "Bad Request", "Not Found"
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
}
