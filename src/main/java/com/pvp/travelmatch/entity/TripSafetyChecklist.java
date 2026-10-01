package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_safety_checklist")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TripSafetyChecklist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_plan_id", nullable = false, unique = true)
    private TravelPlan travelPlan;

    @Column(nullable = false) @Builder.Default private boolean publicMeetingPoint = false;
    @Column(nullable = false) @Builder.Default private boolean itineraryShared = false;
    @Column(nullable = false) @Builder.Default private boolean emergencyContactInformed = false;
    @Column(nullable = false) @Builder.Default private boolean transportConfirmed = false;
    @Column(nullable = false) @Builder.Default private boolean accommodationConfirmed = false;
    @Column(nullable = false) @Builder.Default private boolean offlineMapsReady = false;
    @Column(nullable = false) @Builder.Default private boolean checkInPlanAgreed = false;
    @Column(nullable = false) @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }
}
