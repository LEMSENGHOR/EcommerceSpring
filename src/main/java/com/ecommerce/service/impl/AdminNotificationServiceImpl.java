package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.notification.AdminNotificationFilterRequest;
import com.ecommerce.dto.notification.BroadcastNotificationRequest;
import com.ecommerce.dto.notification.NotificationResponse;
import com.ecommerce.dto.notification.SendNotificationRequest;
import com.ecommerce.entity.Notification;
import com.ecommerce.entity.User;
import com.ecommerce.entity.enums.EntityStatus;
import com.ecommerce.entity.enums.NotificationType;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.NotificationMapper;
import com.ecommerce.repository.NotificationRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.specification.NotificationSpecification;
import com.ecommerce.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public NotificationResponse sendToUser(SendNotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getUserId()));

        Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(parseTypeOrDefault(request.getType(), NotificationType.GENERAL))
                .build();

        return NotificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    public int broadcastToAll(BroadcastNotificationRequest request) {
        NotificationType type = parseTypeOrDefault(request.getType(), NotificationType.PROMOTION);

        // Deliberately simple for this phase's scale: fetches every ACTIVE
        // user and inserts one row per user, synchronously, inside this one
        // request/transaction. Fine for a small-to-medium user base; at real
        // scale this belongs in an async job (e.g. a queued task processed in
        // batches) rather than blocking an admin's HTTP request and holding a
        // single long-running transaction open — flagged again in the README
        // rather than solved here, since that's an infrastructure decision
        // (queue choice, batch size, retry policy) outside this phase's scope.
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == EntityStatus.ACTIVE)
                .toList();

        List<Notification> notifications = activeUsers.stream()
                .map(user -> Notification.builder()
                        .user(user)
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .type(type)
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getAllNotifications(
            AdminNotificationFilterRequest filter, Pageable pageable) {
        Page<Notification> page = notificationRepository.findAll(
                NotificationSpecification.withFilters(filter), pageable);
        return PagedResponse.from(page.map(NotificationMapper::toResponse));
    }

    private NotificationType parseTypeOrDefault(String value, NotificationType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return NotificationType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown notification type: " + value);
        }
    }
}
