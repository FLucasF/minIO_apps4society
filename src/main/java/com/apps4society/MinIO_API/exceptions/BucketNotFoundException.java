package com.apps4society.MinIO_API.exceptions;

public class BucketNotFoundException extends RuntimeException {
    public BucketNotFoundException(String message) {
        super(message);
    }
}
