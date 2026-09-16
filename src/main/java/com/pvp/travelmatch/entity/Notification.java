package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notification_receiver_created",
                        columnList = "receiver_id,created_at"
                ),
                @Index(
                        name = "idx_notification_receiver_read",
                        columnList = "receiver_id,is_read"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "receiver_id",
            nullable = false
    )
    private User receiver;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(
            nullable = false,
            length = 500
    )
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 40
    )
    private NotificationType type;

    private Long relatedEntityId;

    private Boolean isRead;

    private LocalDateTime createdAt;
}