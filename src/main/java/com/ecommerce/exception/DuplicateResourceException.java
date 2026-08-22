package com.ecommerce.exception;

/**
 * Thrown when attempting to create a resource that violates a
 * uniqueness constraint (e.g. category name already exists).
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
