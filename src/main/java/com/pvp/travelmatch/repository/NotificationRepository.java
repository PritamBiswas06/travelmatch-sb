package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.Notification;
import com.pvp.travelmatch.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // All notifications for a user, newest first
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    // Ownership-scoped lookup: a notification is only found if it belongs to
    // this receiver, so it can't be marked read by guessing another user's id.
    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.id = :receiverId AND n.isRead = false")
    int markAllAsReadForReceiver(@Param("receiverId") Long receiverId);

    // Used to prevent PROFILE_VIEW notification spam: true if this exact
    // viewer already notified this exact profile owner of a view since
    // `after` (e.g. now - 24h).
    boolean existsBySenderIdAndReceiverIdAndTypeAndCreatedAtAfter(
            Long senderId,
            Long receiverId,
            NotificationType type,
            LocalDateTime after
    );

    // Used to prevent NEW_MESSAGE notification spam: true if the receiver
    // already has an UNREAD "new message" notification from this exact
    // sender (so several messages sent while the chat is unopened collapse
    // into a single notification).
    boolean existsBySenderIdAndReceiverIdAndTypeAndIsReadFalse(
            Long senderId,
            Long receiverId,
            NotificationType type
    );

    // Called when the receiver opens the conversation with a given sender -
    // clears that sender's unread "new message" notification(s) so the next
    // message they send creates a fresh notification.
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.receiver.id = :receiverId AND n.sender.id = :senderId " +
            "AND n.type = :type AND n.isRead = false")
    int markAsReadBySenderAndReceiverAndType(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId,
            @Param("type") NotificationType type
    );

    // Used to prevent POST_LIKE notification spam: true if the post owner
    // already has an UNREAD "liked your post" notification from this exact
    // liker for this exact post (relatedEntityId = travelPlan id). Scoped by
    // post (not just sender/receiver) so liking a different post still
    // notifies normally.
    boolean existsBySenderIdAndReceiverIdAndTypeAndRelatedEntityIdAndIsReadFalse(
            Long senderId,
            Long receiverId,
            NotificationType type,
            Long relatedEntityId
    );
}
