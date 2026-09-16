package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "idx_partner_user_one", columnList = "user_one_id"),
        @Index(name = "idx_partner_user_two", columnList = "user_two_id"),
        @Index(name = "idx_partner_created_at", columnList = "created_at")
})
public class TravelPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User userOne;

    @ManyToOne
    private User userTwo;

    @ManyToOne
    private TravelPlan travelPlan;

    private LocalDateTime createdAt;
}