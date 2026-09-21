package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        indexes = {
                @Index(name = "idx_reaction_plan_type", columnList = "travel_plan_id,reaction_type"),
                @Index(name = "idx_reaction_user_plan", columnList = "user_id,travel_plan_id")
        },
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"travel_plan_id", "user_id"}
        )
)
public class PostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "travel_plan_id")
    private TravelPlan travelPlan;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String reactionType; // LIKE / DISLIKE

    private LocalDateTime createdAt;
}
