package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="trip_checklist_items", indexes=@Index(name="idx_trip_checklist_plan", columnList="travel_plan_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TripChecklistItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="travel_plan_id", nullable=false) private TravelPlan travelPlan;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="created_by_id", nullable=false) private User createdBy;
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false) @Builder.Default private Boolean completed=false;
    @Column(nullable=false) private LocalDateTime createdAt;
    @PrePersist void createTimestamp(){if(createdAt==null)createdAt=LocalDateTime.now();}
}
