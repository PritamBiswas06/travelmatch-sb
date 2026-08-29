package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository
        extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndReportedUserId(
            Long reporterId,
            Long reportedUserId
    );

    boolean existsByReporterIdAndReportedTravelPlanId(
            Long reporterId,
            Long travelPlanId
    );

    List<Report> findAllByOrderByCreatedAtDesc();
}