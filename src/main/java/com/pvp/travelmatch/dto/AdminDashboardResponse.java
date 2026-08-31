package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long totalTrips;
    private long activeTrips;
    private long totalReports;
    private long pendingReports;
    private long totalReviews;
    private long totalMatchRequests;
    private long totalPartners;
}