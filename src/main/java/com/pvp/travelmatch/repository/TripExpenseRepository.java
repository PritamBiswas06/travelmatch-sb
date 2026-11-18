package com.pvp.travelmatch.repository;
import com.pvp.travelmatch.entity.TripExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TripExpenseRepository extends JpaRepository<TripExpense, Long> {
    List<TripExpense> findByTravelPlanIdOrderByExpenseDateDesc(Long planId);
}
