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
    // notification for the same profile owner. Keeps the dedup rule simple -
    // one existence check against the notifications table, no new tables,
    // no scheduled cleanup job.
    private static final long PROFILE_VIEW_DEDUPLICATION_HOURS = 24;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ==================== Current authenticated user ====================
    // Same pattern as MatchRequestService/UserService: the JWT filter puts
    // the user's email (not an id) into the SecurityContext as the
    // principal. We always resolve the User from THAT, never from anything
    // the client sends.
    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ==================== Create ====================
    // Generic creation entry point reused by every notification-producing
    // feature (match requests, profile views, etc.).
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

    // ==================== Profile view ====================
    // Called from UserService.getProfile() whenever an authenticated user
    // views someone else's profile. Both `profileOwner` and `viewer` are
    // resolved server-side (JWT + path-id lookup) by the caller - this
    // method never receives or trusts a client-supplied id.
    @Transactional
    public void createProfileViewNotification(User profileOwner, User viewer) {

        if (profileOwner.getId().equals(viewer.getId())) {
            return; // never notify a user about viewing their own profile
        }

        LocalDateTime since = LocalDateTime.now().minusHours(PROFILE_VIEW_DEDUPLICATION_HOURS);

        boolean alreadyNotifiedRecently = notificationRepository
                .existsBySenderIdAndReceiverIdAndTypeAndCreatedAtAfter(
                        viewer.getId(),
                        profileOwner.getId(),
                        NotificationType.PROFILE_VIEW,
                        since
                );

        if (alreadyNotifiedRecently) {
            return; // avoid spamming the profile owner on repeated views/refreshes
        }

        createNotification(
                profileOwner,
                viewer,
                "👀 " + viewer.getName() + " viewed your profile.",
                NotificationType.PROFILE_VIEW,
                viewer.getId() // lets the frontend navigate to /profile/{viewerId}
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