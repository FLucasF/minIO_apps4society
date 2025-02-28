package com.apps4society.MinIO_API.exceptions;

public class TooManyRequestsCustomException extends RuntimeException {
    public TooManyRequestsCustomException(String message) {
        super(message);
    }
}
