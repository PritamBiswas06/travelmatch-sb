package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long tripCount;
    private long partnerCount;
    private long requestCount;
    private long unreadNotificationCount;
    private String profilePhotoUrl;

    private List<DashboardTripResponse> plans;
    private List<DashboardPartnerResponse> partners;
    private List<NotificationResponse> recentNotifications;
    private List<DestinationSummaryResponse> destinations;
}