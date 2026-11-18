package com.pvp.travelmatch.repository;
import com.pvp.travelmatch.entity.TripChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TripChecklistItemRepository extends JpaRepository<TripChecklistItem, Long> {
    List<TripChecklistItem> findByTravelPlanIdOrderByCreatedAtAsc(Long planId);
}
