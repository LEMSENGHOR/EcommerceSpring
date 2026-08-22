package com.ecommerce.exception;

/**
 * Thrown when an operation (almost always a delete) is blocked because the
 * resource is still referenced by other data — e.g. deleting a Category that
 * still has Products, or a User with existing Orders. Mapped to 409 Conflict:
 * the request itself is well-formed, but the current state of the data
 * conflicts with it.
 *
 * Distinct from InvalidRequestException (400 — the request's content is
 * semantically wrong, e.g. a sub-category that doesn't belong to the given
 * category) and from DuplicateResourceException (409 — a uniqueness
 * constraint, not a "still referenced" constraint). Introduced in Phase 15
 * specifically to fix a status-code inconsistency: Phase 12's coupon deletion
 * guard used InvalidRequestException (400) for what was always conceptually a
 * 409 — that call site is corrected to use this exception instead.
 */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}
