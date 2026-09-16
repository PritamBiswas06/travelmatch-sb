package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        indexes = {
                @Index(
                        name = "idx_match_receiver_created",
                        columnList = "receiver_id,created_at"
                ),
                @Index(
                        name = "idx_match_sender_created",
                        columnList = "sender_id,created_at"
                )
        },
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "sender_id",
                        "travel_plan_id"
                }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne
    @JoinColumn(name = "travel_plan_id")
    private TravelPlan travelPlan;

    private String status;

    private LocalDateTime createdAt;
}