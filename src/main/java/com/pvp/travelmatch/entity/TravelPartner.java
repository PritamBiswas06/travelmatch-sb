package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(
                name = "idx_partner_user_one_created",
                columnList = "user_one_id,created_at"
        ),
        @Index(
                name = "idx_partner_user_two_created",
                columnList = "user_two_id,created_at"
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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