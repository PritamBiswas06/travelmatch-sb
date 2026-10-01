package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.TripSafetyChecklistRequest;
import com.pvp.travelmatch.dto.TripSafetyChecklistResponse;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.TripSafetyChecklist;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.TripSafetyChecklistRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class TripSafetyChecklistService {
    private final TravelPlanRepository plans;
    private final TripSafetyChecklistRepository checklists;
    private final UserRepository users;

    @Transactional
    public TripSafetyChecklistResponse get(Long planId) {
        User current = currentUser();
        TravelPlan plan = ownedPlan(planId, current);
        TripSafetyChecklist c = checklists.findByTravelPlanId(planId).orElseGet(() ->
                checklists.save(TripSafetyChecklist.builder().travelPlan(plan).updatedAt(LocalDateTime.now()).build()));
        return TripSafetyChecklistResponse.from(c);
    }

    @Transactional
    public TripSafetyChecklistResponse update(Long planId, TripSafetyChecklistRequest request) {
        User current = currentUser();
        TravelPlan plan = ownedPlan(planId, current);
        TripSafetyChecklist c = checklists.findByTravelPlanId(planId).orElseGet(() ->
                TripSafetyChecklist.builder().travelPlan(plan).build());
        c.setPublicMeetingPoint(request.publicMeetingPoint());
        c.setItineraryShared(request.itineraryShared());
        c.setEmergencyContactInformed(request.emergencyContactInformed());
        c.setTransportConfirmed(request.transportConfirmed());
        c.setAccommodationConfirmed(request.accommodationConfirmed());
        c.setOfflineMapsReady(request.offlineMapsReady());
        c.setCheckInPlanAgreed(request.checkInPlanAgreed());
        c.setUpdatedAt(LocalDateTime.now());
        return TripSafetyChecklistResponse.from(checklists.save(c));
    }

    private TravelPlan ownedPlan(Long planId, User user) {
        TravelPlan p = plans.findById(planId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found."));
        if (p.getUser() == null || !p.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the trip owner can manage this safety checklist.");
        }
        return p;
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required.");
        return users.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }
}
