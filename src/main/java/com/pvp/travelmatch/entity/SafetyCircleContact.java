package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_circle_contact", indexes = @Index(name = "idx_safety_contact_owner", columnList = "owner_id,created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SafetyCircleContact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(length = 24)
    private String phone;
    @Column(length = 180)
    private String email;
    @Column(length = 40)
    private String relationship;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist void createTimestamp() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
