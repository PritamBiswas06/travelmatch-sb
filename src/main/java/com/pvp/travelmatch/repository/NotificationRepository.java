package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.Notification;
import com.pvp.travelmatch.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
    findByReceiverIdOrderByCreatedAtDesc(
            Long receiverId
    );

    Page<Notification>
    findByReceiverIdOrderByCreatedAtDesc(
            Long receiverId,
            Pageable pageable
    );

    Optional<Notification>
    findByIdAndReceiverId(
            Long id,
            Long receiverId
    );

    long countByReceiverIdAndIsReadFalse(
            Long receiverId
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.receiver.id = :receiverId
        AND n.isRead = false
    """)
    int markAllAsReadForReceiver(
            @Param("receiverId") Long receiverId
    );

    boolean
    existsBySenderIdAndReceiverIdAndTypeAndCreatedAtAfter(
            Long senderId,
            Long receiverId,
            NotificationType type,
            LocalDateTime after
    );

    boolean
    existsBySenderIdAndReceiverIdAndTypeAndIsReadFalse(
            Long senderId,
            Long receiverId,
            NotificationType type
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.receiver.id = :receiverId
        AND n.sender.id = :senderId
        AND n.type = :type
        AND n.isRead = false
    """)
    int markAsReadBySenderAndReceiverAndType(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId,
            @Param("type") NotificationType type
    );

    boolean
    existsBySenderIdAndReceiverIdAndTypeAndRelatedEntityIdAndIsReadFalse(
            Long senderId,
            Long receiverId,
            NotificationType type,
            Long relatedEntityId
    );
}