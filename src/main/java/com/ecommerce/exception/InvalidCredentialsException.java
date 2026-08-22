package com.ecommerce.exception;

/** Thrown on login when email/password don't match, or a refresh token is invalid/expired. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
