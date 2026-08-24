package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.NotificationResponse;
import com.pvp.travelmatch.entity.Notification;
import com.pvp.travelmatch.entity.NotificationType;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.NotificationRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ==================== Current authenticated user ====================
    // Same pattern as MatchRequestService: the JWT filter puts the user's
    // email (not an id) into the SecurityContext as the principal. We always
    // resolve the User from THAT, never from anything the client sends.
    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ==================== Create ====================
    // Infrastructure for other services to call later (match requests, chat,
    // etc.). Not wired into any existing feature yet - out of scope for now.
    @Transactional
    public Notification createNotification(User receiver, User sender, String message,
                                           NotificationType type, Long relatedEntityId) {

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .message(message)
                .type(type)
                .relatedEntityId(relatedEntityId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    // ==================== Get notifications for logged-in user ====================
    public List<NotificationResponse> getMyNotifications() {

        User currentUser = getCurrentUser();

        return notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    // ==================== Unread count ====================
    public long getUnreadCount() {
        User currentUser = getCurrentUser();
        return notificationRepository.countByReceiverIdAndIsReadFalse(currentUser.getId());
    }

    // ==================== Mark one as read ====================
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {

        User currentUser = getCurrentUser();

        // findByIdAndReceiverId means a notification belonging to someone
        // else simply "doesn't exist" for this user - even if they guess
        // or forge a valid notification id from another account.
        Notification notification = notificationRepository
                .findByIdAndReceiverId(notificationId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);

        return NotificationResponse.fromEntity(notificationRepository.save(notification));
    }

    // ==================== Mark all as read ====================
    @Transactional
    public void markAllAsRead() {
        User currentUser = getCurrentUser();
        notificationRepository.markAllAsReadForReceiver(currentUser.getId());
    }
}