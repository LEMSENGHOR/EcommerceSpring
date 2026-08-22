package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.notification.AdminNotificationFilterRequest;
import com.ecommerce.dto.notification.BroadcastNotificationRequest;
import com.ecommerce.dto.notification.NotificationResponse;
import com.ecommerce.dto.notification.SendNotificationRequest;
import com.ecommerce.service.AdminNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Already restricted to ROLE_ADMIN by SecurityConfig. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
@Tag(name = "Notification (Admin)", description = "Send/broadcast notifications and view send history. ADMIN only.")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendToUser(@Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminNotificationService.sendToUser(request));
    }

    @Operation(summary = "Send the same notification to every ACTIVE user",
            description = "Synchronous: inserts one row per active user inside this single request. Fine "
                    + "at small-to-medium scale; not yet moved to an async batch job, so a very large user "
                    + "base could make this request slow.")
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Integer>> broadcastToAll(
            @Valid @RequestBody BroadcastNotificationRequest request) {
        int sent = adminNotificationService.broadcastToAll(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("notificationsSent", sent));
    }

    /**
     * Examples:
     *   GET /api/admin/notifications?userId=42
     *   GET /api/admin/notifications?type=PAYMENT&isRead=false
     */
    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getAllNotifications(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @PageableDefault(size = 20) Pageable pageable) {

        AdminNotificationFilterRequest filter = AdminNotificationFilterRequest.builder()
                .userId(userId)
                .type(type)
                .isRead(isRead)
                .build();

        return ResponseEntity.ok(adminNotificationService.getAllNotifications(filter, pageable));
    }
}
