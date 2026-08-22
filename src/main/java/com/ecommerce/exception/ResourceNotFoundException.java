package com.ecommerce.exception;

/**
 * Thrown when a requested entity does not exist.
 * Handled centrally in Phase 15 (Exception + Validation); for now
 * GlobalExceptionHandler below maps it to a 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
