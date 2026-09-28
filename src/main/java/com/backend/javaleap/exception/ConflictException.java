package com.backend.javaleap.exception;

/** Thrown when a resource already exists (duplicate username/email...). Maps to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
