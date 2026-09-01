package com.pvp.travelmatch.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="boosted_travel_plans",indexes={@Index(name="idx_boost_plan",columnList="travel_plan_id"),@Index(name="idx_boost_expiry",columnList="expires_at")})
public class BoostedTravelPlan {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="travel_plan_id",nullable=false,unique=true) private TravelPlan travelPlan;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false) private Double multiplier; @Column(nullable=false) private LocalDateTime startedAt; @Column(nullable=false) private LocalDateTime expiresAt; @Column(nullable=false) private LocalDateTime createdAt; @Builder.Default private Boolean active=true;
}
