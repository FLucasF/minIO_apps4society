package com.apps4society.MinIO_API.exceptions;

public class PreconditionRequiredCustomException extends RuntimeException {
    public PreconditionRequiredCustomException(String message) {
        super(message);
    }
}
