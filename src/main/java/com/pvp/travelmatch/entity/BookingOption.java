package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_booking_options", indexes = {
    @Index(name = "idx_booking_option_trip", columnList = "travel_plan_id"),
    @Index(name = "idx_booking_option_trip_category", columnList = "travel_plan_id,category")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    @Column(nullable = false, length = 24) private String category;
    @Column(nullable = false, length = 160) private String providerName;
    @Column(length = 1000) private String bookingUrl;
    @Column(precision = 12, scale = 2) private BigDecimal estimatedPrice;
    @Column(nullable = false, length = 3) @Builder.Default private String currency = "INR";
    private Integer travelersCovered;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    @Column(length = 1500) private String notes;
    @Column(nullable = false, length = 24) @Builder.Default private String status = "CONSIDERING";
    @Column(nullable = false) @Builder.Default private Boolean preferred = false;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void createTimestamps() { LocalDateTime now=LocalDateTime.now(); if(createdAt==null)createdAt=now; if(updatedAt==null)updatedAt=now; }
    @PreUpdate void updateTimestamp() { updatedAt=LocalDateTime.now(); }
}
