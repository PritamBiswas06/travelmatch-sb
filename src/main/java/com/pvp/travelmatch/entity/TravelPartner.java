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