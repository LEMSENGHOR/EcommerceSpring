package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.notification.AdminNotificationFilterRequest;
import com.ecommerce.dto.notification.BroadcastNotificationRequest;
import com.ecommerce.dto.notification.NotificationResponse;
import com.ecommerce.dto.notification.SendNotificationRequest;
import org.springframework.data.domain.Pageable;

public interface AdminNotificationService {

    NotificationResponse sendToUser(SendNotificationRequest request);

    /** Creates one notification per ACTIVE user. See README for the scale caveat. */
    int broadcastToAll(BroadcastNotificationRequest request);

    PagedResponse<NotificationResponse> getAllNotifications(AdminNotificationFilterRequest filter, Pageable pageable);
}
