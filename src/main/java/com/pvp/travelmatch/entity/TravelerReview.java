package com.pvp.travelmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "reviewer_id",
                        "reviewed_user_id",
                        "travel_plan_id"
                }
        )
)
public class TravelerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "reviewer_id",
            nullable = false
    )
    private User reviewer;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "reviewed_user_id",
            nullable = false
    )
    private User reviewedUser;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "travel_plan_id",
            nullable = false
    )
    private TravelPlan travelPlan;

    @Column(nullable = false)
    private Integer rating;

    @ElementCollection
    @CollectionTable(
            name = "traveler_review_tags",
            joinColumns =
            @JoinColumn(name = "review_id")
    )
    @Column(name = "tag", length = 40)
    @Builder.Default
    private List<String> tags =
            new ArrayList<>();

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}