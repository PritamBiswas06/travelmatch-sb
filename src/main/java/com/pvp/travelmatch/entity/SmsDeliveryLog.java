package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_delivery_log", indexes = {
        @Index(name = "idx_sms_delivery_created_at", columnList = "created_at"),
        @Index(name = "idx_sms_delivery_status", columnList = "delivery_status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmsDeliveryLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "recipient_masked", nullable = false, length = 24)
    private String recipientMasked;

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(name = "provider_error_code", length = 40)
    private String providerErrorCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void beforeInsert() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
