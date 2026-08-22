package com.ecommerce.exception;

/** Thrown when an operation requires an authenticated user but none is present. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
