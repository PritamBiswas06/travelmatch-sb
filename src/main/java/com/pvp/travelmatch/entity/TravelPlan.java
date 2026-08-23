package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
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

    private String travelType; // Adventure / Religious / Leisure

    private LocalDateTime createdAt;

    // ACTIVE / COMPLETED / CANCELLED - defaults to ACTIVE at creation
    @Builder.Default
    private String status = "ACTIVE";

    // Raw counter, incremented via /share endpoint
    @Builder.Default
    private Integer shareCount = 0;

    // Link with User
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}