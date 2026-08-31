package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_travel_plans",
        uniqueConstraints = @UniqueConstraint(name = "uk_saved_user_plan", columnNames = {"user_id", "travel_plan_id"}),
        indexes = {
                @Index(name = "idx_saved_user", columnList = "user_id"),
                @Index(name = "idx_saved_plan", columnList = "travel_plan_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedTravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
