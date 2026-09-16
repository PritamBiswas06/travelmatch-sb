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