package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_expenses", indexes = @Index(name="idx_trip_expense_trip", columnList="travel_plan_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TripExpense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="travel_plan_id", nullable=false) private TravelPlan travelPlan;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="paid_by_id", nullable=false) private User paidBy;
    @Column(nullable=false, length=160) private String description;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
    @Column(nullable=false, length=3) @Builder.Default private String currency="INR";
    @Column(nullable=false, length=24) @Builder.Default private String category="OTHER";
    @Column(nullable=false) private LocalDate expenseDate;
    @Column(nullable=false) private LocalDateTime createdAt;
    @PrePersist void createTimestamp(){ if(createdAt==null)createdAt=LocalDateTime.now(); }
}
