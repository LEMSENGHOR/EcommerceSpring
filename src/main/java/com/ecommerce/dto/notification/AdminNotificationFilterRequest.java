package com.ecommerce.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bound from query params on GET /api/admin/notifications. All fields optional. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminNotificationFilterRequest {
    private Long userId;
    private String type;
    private Boolean isRead;
}
