package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who this notification is FOR. Always resolved server-side from the JWT
    // user, never from a client-supplied userId.
    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // Who triggered this notification. Nullable, since system notifications
    // (e.g. "Your account was verified") have no sender.
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    // Optional id of the entity this notification is about
    // (e.g. a MatchRequest id, TravelPlan id, Message id).
    // Meaning depends on `type`; frontend uses it to deep-link.
    private Long relatedEntityId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}