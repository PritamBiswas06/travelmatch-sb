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
    private long totalPartners; private long premiumUsers; private long activeSubscriptions; private long totalPayments; private long successfulPayments; private long failedPayments; private long totalBoosts; private long activeBoosts; private long revenuePaise;
}