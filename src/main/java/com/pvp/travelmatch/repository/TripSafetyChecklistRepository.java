package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TripSafetyChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TripSafetyChecklistRepository extends JpaRepository<TripSafetyChecklist, Long> {
    Optional<TripSafetyChecklist> findByTravelPlanId(Long travelPlanId);
}
