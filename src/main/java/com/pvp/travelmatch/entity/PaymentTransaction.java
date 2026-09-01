package com.pvp.travelmatch.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="payment_transactions",indexes={@Index(name="idx_payment_user",columnList="user_id"),@Index(name="idx_payment_order",columnList="provider_order_id"),@Index(name="idx_payment_status",columnList="status")})
public class PaymentTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentProvider provider;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentType type;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) @Builder.Default private PaymentStatus status=PaymentStatus.CREATED;
 @Column(name="provider_order_id",nullable=false,unique=true,length=100) private String providerOrderId;
 @Column(name="provider_payment_id",unique=true,length=100) private String providerPaymentId;
 @Column(nullable=false) private Long amount; @Column(nullable=false,length=3) private String currency; private Long referenceId;
 @Column(length=1000) private String failureReason; @Column(nullable=false) private LocalDateTime createdAt; @Column(nullable=false) private LocalDateTime updatedAt;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();if(updatedAt==null)updatedAt=createdAt;} @PreUpdate void onUpdate(){updatedAt=LocalDateTime.now();}
}
