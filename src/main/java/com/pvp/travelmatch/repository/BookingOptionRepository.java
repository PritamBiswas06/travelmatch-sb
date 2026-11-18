package com.pvp.travelmatch.repository;
import com.pvp.travelmatch.entity.BookingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BookingOptionRepository extends JpaRepository<BookingOption, Long> {
    List<BookingOption> findByTravelPlanIdOrderByCreatedAtDesc(Long planId);
}
