package com.ecommerce.security;

import com.ecommerce.dto.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Ensures unauthenticated (401) and forbidden (403) requests get the same
 * ErrorResponse shape as GlobalExceptionHandler, instead of Spring Security's
 * default HTML/empty-body responses — these fire before the DispatcherServlet
 * hands off to @RestControllerAdvice (they're rejected in the filter chain,
 * e.g. a missing/invalid JWT), so they need their own handlers rather than
 * relying on GlobalExceptionHandler's AccessDeniedException handler, which
 * only catches denials thrown *inside* a controller/service method.
 *
 * Refactored in Phase 15 to build the shared ErrorResponse DTO instead of a
 * hand-rolled map, so this and GlobalExceptionHandler can never drift into
 * two different error shapes again.
 */
@Component
public class SecurityResponseHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    // Built once, not per-request — JavaTimeModule registration is the only
    // per-call cost the previous version paid unnecessarily.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        writeError(request, response, HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        writeError(request, response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                             HttpStatus status, String message) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
