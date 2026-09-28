package com.backend.javaleap.exception;

/** Too many failed logins - account temporarily locked. Maps to HTTP 429. */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }
}
