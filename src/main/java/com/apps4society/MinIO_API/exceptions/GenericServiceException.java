package com.apps4society.MinIO_API.exceptions;

public class GenericServiceException extends RuntimeException {
    public GenericServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
