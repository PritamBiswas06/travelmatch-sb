package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.AnalyticsEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {
    List<AnalyticsEvent> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
    List<AnalyticsEvent> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(LocalDateTime from);
    Page<AnalyticsEvent> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<AnalyticsEvent> findByUser_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(Long userId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    long countByEventTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(String eventType, LocalDateTime from, LocalDateTime to);
    @Modifying @Query("delete from AnalyticsEvent e where e.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
