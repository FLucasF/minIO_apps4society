package com.apps4society.MinIO_API.exceptions;

public class DuplicateFileException extends RuntimeException {
    public DuplicateFileException(String message) {
        super(message);
    }
}
