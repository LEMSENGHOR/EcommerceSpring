package com.ecommerce.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin sends the same notification to every ACTIVE user. See README for the
 * scale caveat — this creates one row per user synchronously within the
 * request, which is fine for a small user base but should move to an async
 * batch job before this project has thousands of users.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastNotificationRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 5000, message = "message must be at most 5000 characters")
    private String message;

    /** Defaults to PROMOTION if omitted — broadcasts are almost always promotional. */
    private String type;
}
