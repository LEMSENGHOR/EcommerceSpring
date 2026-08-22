package com.ecommerce.exception;

import com.ecommerce.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The single place every exception in this API becomes an HTTP response.
 * Built up incrementally since Phase 3 (each new exception type added its
 * own handler as it was introduced); this phase consolidates all of them
 * onto one consistent ErrorResponse shape and closes the remaining gaps:
 * malformed request bodies, bad path/query params, raw DB constraint
 * violations that slipped past a service-layer pre-check, and — critically —
 * a catch-all so an unexpected bug returns a controlled 500 instead of
 * leaking a stack trace via Spring Boot's default error page.
 *
 * Ordering note: Spring resolves @ExceptionHandler methods by most-specific
 * exception type first, so a subclass-specific handler here always wins over
 * a broader one (e.g. ResourceNotFoundException over the catch-all Exception
 * handler) regardless of method declaration order in this file.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------------------------------------
    // Domain exceptions (this project's own, thrown deliberately by services)
    // ---------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ErrorResponse> handleResourceInUse(ResourceInUseException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    // ---------------------------------------------------------
    // Spring Security exceptions raised outside the JWT filter chain
    // (e.g. from AuthenticationManager.authenticate() inside AuthServiceImpl.login,
    // or @PreAuthorize-style access checks)
    // ---------------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request, null);
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ErrorResponse> handleAccountDisabled(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "This account is not active. Please contact support.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request, null);
    }

    // ---------------------------------------------------------
    // Request-shape problems — malformed input Spring itself rejects before
    // a controller method (or @Valid) even runs
    // ---------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    /** Unreadable/malformed JSON body — e.g. a trailing comma, wrong type for a field, empty body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request, null);
    }

    /** e.g. GET /api/products/{id} called with id=abc instead of a number. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Parameter '" + ex.getName() + "' has an invalid value: '" + ex.getValue() + "'";
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    /** e.g. a required @RequestParam left off the query string entirely. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName(), request, null);
    }

    /**
     * A valid path hit with the wrong HTTP method (e.g. DELETE on an endpoint
     * that only supports GET/POST). Without this handler these fall through
     * to the generic Exception catch-all below and misreport as a 500 — this
     * exception isn't a RuntimeException (it's a checked ServletException
     * subclass), which is easy to overlook when wiring up handlers one at a
     * time, but @ExceptionHandler catches it here regardless.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = "Method '" + ex.getMethod() + "' is not supported for this endpoint";
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, request, null);
    }

    /**
     * A URL with no matching @RequestMapping at all (a typo'd path, a
     * resource id under the wrong prefix, etc). Only fires because
     * application.yml sets spring.mvc.throw-exception-if-no-handler-found and
     * spring.web.resources.add-mappings=false — by default Spring Boot
     * silently serves its own whitelabel 404 for this case, bypassing this
     * handler (and its consistent ErrorResponse shape) entirely.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL(), request, null);
    }

    // ---------------------------------------------------------
    // Safety net — a raw DB constraint violation that reached this handler
    // means some service method's delete/update didn't pre-check a
    // reference the way CategoryServiceImpl/BrandServiceImpl/ProductServiceImpl/
    // AdminUserServiceImpl/AdminCouponServiceImpl now do (see README). This
    // handler is deliberately generic and a fallback, not a substitute for
    // those pre-checks — a pre-check gives a specific, actionable message;
    // this only prevents an unhandled case from ever leaking as a raw 500.
    // ---------------------------------------------------------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Unhandled data integrity violation at {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "This action could not be completed because the record is referenced by other data",
                request, null);
    }

    // ---------------------------------------------------------
    // Catch-all — anything not handled above is a bug, not an expected
    // outcome. Logged with the full stack trace server-side; the client
    // gets a generic message only, never the exception's own message or
    // trace (which could leak internal details).
    // ---------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> fieldErrors) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request != null ? request.getRequestURI() : null)
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
