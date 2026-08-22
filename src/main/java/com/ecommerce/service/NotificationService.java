package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.notification.NotificationResponse;
import com.ecommerce.dto.notification.UnreadCountResponse;
import com.ecommerce.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // ---------------------------------------------------------
    // Self-service — every method acts on the currently authenticated user
    // ---------------------------------------------------------

    PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable);

    UnreadCountResponse getUnreadCount();

    NotificationResponse markAsRead(Long notificationId);

    void markAllAsRead();

    void deleteNotification(Long notificationId);

    // ---------------------------------------------------------
    // Internal — called by other services as a side effect of business
    // events (order placed, payment processed, etc.), never exposed via any
    // controller directly. Deliberately swallows its own failures (logs and
    // returns) rather than throwing — a notification failing to send must
    // never roll back the order/payment transaction that triggered it.
    // ---------------------------------------------------------

    void notify(Long userId, String title, String message, NotificationType type);
}
