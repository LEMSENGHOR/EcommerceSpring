package com.ecommerce.exception;

/**
 * Thrown when request data is well-formed but semantically invalid
 * (e.g. a subCategoryId that doesn't belong to the given categoryId).
 * Distinct from bean-validation errors, which are handled separately.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
