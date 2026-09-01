package com.pvp.travelmatch.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="subscriptions", indexes={@Index(name="idx_subscription_user",columnList="user_id"),@Index(name="idx_subscription_status",columnList="status")})
public class Subscription {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false,unique=true) private User user;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) @Builder.Default private SubscriptionPlan plan=SubscriptionPlan.FREE;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) @Builder.Default private SubscriptionStatus status=SubscriptionStatus.EXPIRED;
 @Enumerated(EnumType.STRING) @Column(length=20) private PaymentProvider provider;
 @Column(length=100) private String providerSubscriptionId;
 private LocalDateTime startDate; private LocalDateTime endDate; @Builder.Default private Boolean autoRenew=false;
 @Column(nullable=false) private LocalDateTime createdAt; @Column(nullable=false) private LocalDateTime updatedAt;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();if(updatedAt==null)updatedAt=createdAt;} @PreUpdate void onUpdate(){updatedAt=LocalDateTime.now();}
}
