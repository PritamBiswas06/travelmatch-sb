package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "analytics_events", indexes = {
        @Index(name = "idx_analytics_created_at", columnList = "created_at"),
        @Index(name = "idx_analytics_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_analytics_session", columnList = "session_id"),
        @Index(name = "idx_analytics_event_type", columnList = "event_type,created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "page_path", length = 180)
    private String pagePath;

    // A controlled, non-sensitive feature label; never free-form user content.
    @Column(name = "event_label", length = 80)
    private String eventLabel;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
