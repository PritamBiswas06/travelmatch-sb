package com.pvp.travelmatch.specification;

import com.pvp.travelmatch.dto.FeedFilterRequest;
import com.pvp.travelmatch.entity.TravelPlan;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import com.pvp.travelmatch.entity.BlockedUser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Builds the dynamic WHERE clause for the feed query so all filters are
// applied at the database level in a single query, rather than fetching
// everything and filtering in memory.
public final class TravelPlanSpecifications {

    private TravelPlanSpecifications() {
    }

    public static Specification<TravelPlan> feedFilters(Long viewerId, LocalDate today, FeedFilterRequest filter) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Base feed rules (unchanged from the original findFeedPlans query):
            // other users' active, non-expired trips.
            predicates.add(cb.notEqual(root.get("user").get("id"), viewerId));

            var blockedSubquery =
                    query.subquery(Long.class);

            var blockedRoot =
                    blockedSubquery.from(BlockedUser.class);

            blockedSubquery.select(
                    cb.literal(1L)
            );

            blockedSubquery.where(
                    cb.equal(
                            blockedRoot
                                    .get("blocker")
                                    .get("id"),
                            viewerId
                    ),

                    cb.equal(
                            blockedRoot
                                    .get("blockedUser")
                                    .get("id"),
                            root.get("user").get("id")
                    )
            );

            predicates.add(
                    cb.not(
                            cb.exists(blockedSubquery)
                    )
            );

            predicates.add(cb.equal(root.get("status"), "ACTIVE"));
            predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), today));

            if (filter != null) {

                if (hasText(filter.getDestination())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("destination")),
                            "%" + filter.getDestination().trim().toLowerCase() + "%"
                    ));
                }

                if (hasText(filter.getFromLocation())) {
                    predicates.add(cb.like(
                            cb.lower(root.get("fromLocation")),
                            "%" + filter.getFromLocation().trim().toLowerCase() + "%"
                    ));
                }

                if (filter.getMinBudget() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("budget"), filter.getMinBudget()));
                }

                if (filter.getMaxBudget() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("budget"), filter.getMaxBudget()));
                }

                if (filter.getMinAge() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("user").get("age"), filter.getMinAge()));
                if (filter.getMaxAge() != null) predicates.add(cb.lessThanOrEqualTo(root.get("user").get("age"), filter.getMaxAge()));
                if (hasText(filter.getTravelStyle())) predicates.add(cb.like(cb.lower(root.get("user").get("travelStyle")), "%"+filter.getTravelStyle().trim().toLowerCase()+"%"));
                if (hasText(filter.getTravelInterest())) predicates.add(cb.like(cb.lower(root.get("user").get("travelInterests")), "%"+filter.getTravelInterest().trim().toLowerCase()+"%"));
                if (hasText(filter.getLanguage())) predicates.add(cb.like(cb.lower(root.get("user").get("languages")), "%"+filter.getLanguage().trim().toLowerCase()+"%"));
                if (hasText(filter.getCountry())) predicates.add(cb.equal(cb.lower(root.get("user").get("country")), filter.getCountry().trim().toLowerCase()));
                if (hasText(filter.getCity())) predicates.add(cb.equal(cb.lower(root.get("user").get("city")), filter.getCity().trim().toLowerCase()));

                if (hasText(filter.getTravelType())) {
                    predicates.add(cb.equal(
                            cb.lower(root.get("travelType")),
                            filter.getTravelType().trim().toLowerCase()
                    ));
                }

                // Date-range overlap: only constrain the side(s) the user actually provided.
                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), filter.getStartDate()));
                }

                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), filter.getEndDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}