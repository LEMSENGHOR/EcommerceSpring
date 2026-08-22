package com.ecommerce.exception;

/**
 * Thrown when a checkout can't proceed because requested quantity exceeds
 * available stock, detected at the atomic decrement (not a pre-check read).
 * Mapped to 409 — the request itself was valid, but current inventory state
 * conflicts with it.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
