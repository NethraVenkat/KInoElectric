package com.backend.javaleap.exception;

/** Thrown for invalid business input. Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
