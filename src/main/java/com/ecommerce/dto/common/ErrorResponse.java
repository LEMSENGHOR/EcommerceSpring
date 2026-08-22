package com.ecommerce.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The single shape every error response takes across the whole API, from
 * this phase on — used by GlobalExceptionHandler for everything from a 404
 * to a validation failure to an unexpected 500. Before Phase 15 each handler
 * built its own ad-hoc LinkedHashMap; consolidating onto one DTO here so the
 * contract is stable and documentable (Phase 16, Swagger, describes this
 * shape once instead of guessing at each endpoint's error format).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;

    /** Request path that produced the error, e.g. "/api/products/42". Omitted where not available. */
    private String path;

    /** Only present for field-level validation failures (400 from @Valid). */
    private Map<String, String> fieldErrors;
}
