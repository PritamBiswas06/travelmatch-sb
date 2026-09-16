package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.DashboardPartnerResponse;
import com.pvp.travelmatch.dto.DashboardSummaryResponse;
import com.pvp.travelmatch.dto.DashboardTripResponse;
import com.pvp.travelmatch.dto.DestinationSummaryResponse;
import com.pvp.travelmatch.dto.NotificationResponse;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.MatchRequestRepository;
import com.pvp.travelmatch.repository.NotificationRepository;
import com.pvp.travelmatch.repository.TravelPartnerRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_TRIP_LIMIT = 4;
    private static final int RECENT_PARTNER_LIMIT = 4;
    private static final int RECENT_NOTIFICATION_LIMIT = 5;
    private static final int DESTINATION_LIMIT = 4;

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final TravelPartnerRepository travelPartnerRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {

        User user = getCurrentUser();
        Long userId = user.getId();

        PageRequest recentTripsPage = PageRequest.of(
                0,
                RECENT_TRIP_LIMIT,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        PageRequest recentPartnersPage = PageRequest.of(
                0,
                RECENT_PARTNER_LIMIT,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        PageRequest recentNotificationsPage = PageRequest.of(
                0,
                RECENT_NOTIFICATION_LIMIT,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        /*
         * Only load the small amount of data actually required by the
         * dashboard. The complete datasets are NOT loaded here.
         */
        List<DashboardTripResponse> plans =
                travelPlanRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                userId,
                                recentTripsPage
                        )
                        .getContent()
                        .stream()
                        .map(DashboardTripResponse::fromEntity)
                        .toList();

        List<DashboardPartnerResponse> partners =
                travelPartnerRepository
                        .findByUserOneIdOrUserTwoIdOrderByCreatedAtDesc(
                                userId,
                                userId,
                                recentPartnersPage
                        )
                        .getContent()
                        .stream()
                        .map(partner ->
                                DashboardPartnerResponse.fromEntity(
                                        partner,
                                        userId
                                )
                        )
                        .toList();

        List<NotificationResponse> notifications =
                notificationRepository
                        .findByReceiverIdOrderByCreatedAtDesc(
                                userId,
                                recentNotificationsPage
                        )
                        .getContent()
                        .stream()
                        .map(NotificationResponse::fromEntity)
                        .toList();

        List<DestinationSummaryResponse> destinations =
                travelPlanRepository
                        .findDestinationCountsByUserId(
                                userId,
                                PageRequest.of(0, DESTINATION_LIMIT)
                        )
                        .stream()
                        .map(row ->
                                new DestinationSummaryResponse(
                                        String.valueOf(row[0]),
                                        ((Number) row[1]).longValue()
                                )
                        )
                        .toList();

        return new DashboardSummaryResponse(
                travelPlanRepository.countByUserId(userId),
                travelPartnerRepository.countByUserOneIdOrUserTwoId(
                        userId,
                        userId
                ),
                matchRequestRepository.countByReceiverId(userId),
                notificationRepository.countByReceiverIdAndIsReadFalse(
                        userId
                ),
                plans,
                partners,
                notifications,
                destinations
        );
    }

    private User getCurrentUser() {

        String email =
                (String) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }
}