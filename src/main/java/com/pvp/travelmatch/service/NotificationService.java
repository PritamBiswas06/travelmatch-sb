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

    // Minimum gap before the same viewer can trigger another PROFILE_VIEW
    // notification for the same profile owner.
    private static final long PROFILE_VIEW_DEDUPLICATION_HOURS = 24;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Web Push service used to deliver browser/mobile push notifications.
    private final WebPushService webPushService;

    // ==================== Current authenticated user ====================

    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ==================== Create ====================

    // Generic creation entry point reused by every notification-producing
    // feature such as match requests, profile views, chat messages and likes.
    @Transactional
    public Notification createNotification(
            User receiver,
            User sender,
            String message,
            NotificationType type,
            Long relatedEntityId) {

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .message(message)
                .type(type)
                .relatedEntityId(relatedEntityId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // IMPORTANT:
        // Save the normal in-app notification first.
        Notification saved = notificationRepository.save(notification);

        // Then attempt Web Push.
        //
        // Push failure must NEVER break the main action or prevent
        // the database notification from being created.
        try {
            webPushService.sendToUser(
                    receiver,
                    "TravelMatch",
                    message,
                    type,
                    relatedEntityId
            );
        } catch (Exception e) {
            // Intentionally ignored.
            // The in-app notification has already been saved successfully.
            System.err.println(
                    "Web Push notification failed: " + e.getMessage()
            );
        }

        return saved;
    }

    // ==================== Profile view ====================

    @Transactional
    public void createProfileViewNotification(
            User profileOwner,
            User viewer) {

        if (profileOwner.getId().equals(viewer.getId())) {
            return;
        }

        LocalDateTime since = LocalDateTime.now()
                .minusHours(PROFILE_VIEW_DEDUPLICATION_HOURS);

        boolean alreadyNotifiedRecently =
                notificationRepository
                        .existsBySenderIdAndReceiverIdAndTypeAndCreatedAtAfter(
                                viewer.getId(),
                                profileOwner.getId(),
                                NotificationType.PROFILE_VIEW,
                                since
                        );

        if (alreadyNotifiedRecently) {
            return;
        }

        createNotification(
                profileOwner,
                viewer,
                "👀 " + viewer.getName() + " viewed your profile.",
                NotificationType.PROFILE_VIEW,
                viewer.getId()
        );
    }

    // ==================== Chat message ====================

    @Transactional
    public void createChatMessageNotification(
            User receiver,
            User sender) {

        if (receiver.getId().equals(sender.getId())) {
            return;
        }

        boolean alreadyHasUnreadNotification =
                notificationRepository
                        .existsBySenderIdAndReceiverIdAndTypeAndIsReadFalse(
                                sender.getId(),
                                receiver.getId(),
                                NotificationType.NEW_MESSAGE
                        );

        if (alreadyHasUnreadNotification) {
            return;
        }

        createNotification(
                receiver,
                sender,
                "💬 " + sender.getName() + " sent you a message.",
                NotificationType.NEW_MESSAGE,
                sender.getId()
        );
    }

    // Called when a user opens a chat.
    @Transactional
    public void markMessageNotificationsAsRead(
            Long receiverId,
            Long senderId) {

        notificationRepository.markAsReadBySenderAndReceiverAndType(
                receiverId,
                senderId,
                NotificationType.NEW_MESSAGE
        );
    }

    // ==================== Post like ====================

    @Transactional
    public void createPostLikeNotification(
            User postOwner,
            User liker,
            Long travelPlanId) {

        if (postOwner.getId().equals(liker.getId())) {
            return;
        }

        boolean alreadyHasUnreadNotification =
                notificationRepository
                        .existsBySenderIdAndReceiverIdAndTypeAndRelatedEntityIdAndIsReadFalse(
                                liker.getId(),
                                postOwner.getId(),
                                NotificationType.POST_LIKE,
                                travelPlanId
                        );

        if (alreadyHasUnreadNotification) {
            return;
        }

        createNotification(
                postOwner,
                liker,
                "❤️ " + liker.getName() + " liked your travel post.",
                NotificationType.POST_LIKE,
                travelPlanId
        );
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

        return notificationRepository
                .countByReceiverIdAndIsReadFalse(currentUser.getId());
    }

    // ==================== Delete one ====================

    @Transactional
    public void delete(Long notificationId) {
        User currentUser = getCurrentUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getReceiver().getId().equals(currentUser.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "You can only delete your own notifications");
        }

        notificationRepository.delete(notification);
    }

    // ==================== Mark one as read ====================

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {

        User currentUser = getCurrentUser();

        Notification notification =
                notificationRepository
                        .findByIdAndReceiverId(
                                notificationId,
                                currentUser.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Notification not found"
                                )
                        );

        notification.setIsRead(true);

        return NotificationResponse.fromEntity(
                notificationRepository.save(notification)
        );
    }

    // ==================== Mark all as read ====================

    @Transactional
    public void markAllAsRead() {

        User currentUser = getCurrentUser();

        notificationRepository.markAllAsReadForReceiver(
                currentUser.getId()
        );
    }
}