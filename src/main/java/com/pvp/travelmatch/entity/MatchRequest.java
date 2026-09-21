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
                @Index(name = "idx_match_receiver_created", columnList = "receiver_id,created_at"),
                @Index(name = "idx_match_sender_created", columnList = "sender_id,created_at"),
                @Index(name = "idx_match_plan", columnList = "travel_plan_id")
        },
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"sender_id", "travel_plan_id"}
        )
)
public class MatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who sent request
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    // Who received request
    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    // Travel Plan involved
    @ManyToOne
    @JoinColumn(name = "travel_plan_id")
    private TravelPlan travelPlan;

    private String status; // PENDING / ACCEPTED / REJECTED

    private LocalDateTime createdAt;
}