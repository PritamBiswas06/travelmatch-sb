package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.Report;
import com.pvp.travelmatch.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long reportedUserId);
    boolean existsByReporterIdAndReportedTravelPlanId(Long reporterId, Long reportedTravelPlanId);

    List<Report> findAllByOrderByCreatedAtDesc();

    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
    long countByStatus(ReportStatus status);
}
