package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}