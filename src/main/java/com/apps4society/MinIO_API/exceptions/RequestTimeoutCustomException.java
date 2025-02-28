package com.apps4society.MinIO_API.exceptions;

public class RequestTimeoutCustomException extends RuntimeException {
    public RequestTimeoutCustomException(String message) {
        super(message);
    }
}
