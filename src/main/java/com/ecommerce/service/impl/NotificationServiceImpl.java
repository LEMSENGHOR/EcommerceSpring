package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.notification.NotificationResponse;
import com.ecommerce.dto.notification.UnreadCountResponse;
import com.ecommerce.entity.Notification;
import com.ecommerce.entity.User;
import com.ecommerce.entity.enums.NotificationType;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.NotificationMapper;
import com.ecommerce.repository.NotificationRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                SecurityUtils.getCurrentUserId(), pageable);
        return PagedResponse.from(page.map(NotificationMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        long count = notificationRepository.countByUserIdAndIsReadFalse(SecurityUtils.getCurrentUserId());
        return UnreadCountResponse.builder().count(count).build();
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = findOwnedNotificationOrThrow(notificationId);
        notification.setIsRead(true);
        return NotificationMapper.toResponse(notification);
    }

    @Override
    public void markAllAsRead() {
        notificationRepository.findByUserIdAndIsReadFalse(SecurityUtils.getCurrentUserId())
                .forEach(n -> n.setIsRead(true));
    }

    @Override
    public void deleteNotification(Long notificationId) {
        Notification notification = findOwnedNotificationOrThrow(notificationId);
        notificationRepository.delete(notification);
    }

    @Override
    // REQUIRES_NEW: runs in its own transaction, independent of whatever
    // caller (order placement, payment processing, ...) invoked this. Combined
    // with the try/catch below, a notification failure can never roll back —
    // or even be affected by — the business transaction that triggered it.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(Long userId, String title, String message, NotificationType type) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            notificationRepository.save(Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(type != null ? type : NotificationType.GENERAL)
                    .build());
            // Note: Notification uses GenerationType.IDENTITY (like every entity
            // in this project), so save() issues its INSERT synchronously rather
            // than deferring to flush/commit — any DB failure surfaces here,
            // inside this try block, not after this method has already returned
            // control to the caller. That's what makes catching Exception here
            // actually sufficient to guarantee the caller's transaction is
            // unaffected, rather than just usually working.
        } catch (Exception e) {
            // Deliberately swallowed — see interface javadoc. A failed
            // notification is a UX gap, not a reason to fail an order or
            // payment. Logged at WARN so it's visible without being noisy.
            log.warn("Failed to create notification for user {}: {}", userId, e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Notification findOwnedNotificationOrThrow(Long notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + notificationId));
    }
}
