package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "smart_trip_matches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_smart_trip_match_plans",
                columnNames = {"plan_a_id", "plan_b_id"}
        ),
        indexes = {
                @Index(name = "idx_smart_match_plan_a", columnList = "plan_a_id"),
                @Index(name = "idx_smart_match_plan_b", columnList = "plan_b_id"),
                @Index(name = "idx_smart_match_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartTripMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Canonical lower-id plan so the same pair can never be stored twice. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_a_id", nullable = false)
    private TravelPlan planA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_b_id", nullable = false)
    private TravelPlan planB;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    @Builder.Default
    private Boolean dismissedByA = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean dismissedByB = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean viewedByA = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean viewedByB = false;

    /** Set only after the first pair of match notifications has been sent. */
    private LocalDateTime notifiedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
