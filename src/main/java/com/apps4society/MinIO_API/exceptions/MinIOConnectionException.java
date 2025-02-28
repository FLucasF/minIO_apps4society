package com.apps4society.MinIO_API.exceptions;

public class MinIOConnectionException extends RuntimeException {
    public MinIOConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
