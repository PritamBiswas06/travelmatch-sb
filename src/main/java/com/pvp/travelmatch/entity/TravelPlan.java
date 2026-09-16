package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(
                name = "idx_travel_plan_user_created",
                columnList = "user_id,created_at"
        ),
        @Index(
                name = "idx_travel_plan_user_destination",
                columnList = "user_id,destination"
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromLocation;

    private String destination;

    private LocalDate startDate;

    private LocalDate endDate;

    private Double budget;

    private String travelType;

    private LocalDateTime createdAt;

    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private Integer shareCount = 0;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}