package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.TravelPartnerResponse;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.pvp.travelmatch.repository.TravelPartnerRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TravelPartnerService {
    public final UserRepository userRepository;
    public final TravelPartnerRepository travelPartnerRepository;
    public Page<TravelPartnerResponse> getMyPartners(int page, int size) {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        int safeSize = Math.min(Math.max(size, 1), 20);
        return travelPartnerRepository
                .findByUserOneIdOrUserTwoIdOrderByCreatedAtDesc(
                        user.getId(), user.getId(),
                        PageRequest.of(Math.max(page, 0), safeSize)
                )
                .map(partner -> {
                    User one = partner.getUserOne();
                    User two = partner.getUserTwo();
                    return new TravelPartnerResponse(
                            partner.getId(),
                            new TravelPartnerResponse.UserSummary(one.getId(), one.getName(), one.getCity(), one.getCountry()),
                            new TravelPartnerResponse.UserSummary(two.getId(), two.getName(), two.getCity(), two.getCountry()),
                            new TravelPartnerResponse.TravelPlanSummary(
                                    partner.getTravelPlan().getId(),
                                    partner.getTravelPlan().getDestination(),
                                    partner.getTravelPlan().getStartDate(),
                                    partner.getTravelPlan().getEndDate()
                            ),
                            partner.getCreatedAt()
                    );
                });
    }
}
