package com.pvp.travelmatch.dto;

import com.pvp.travelmatch.entity.TravelPartner;
import com.pvp.travelmatch.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPartnerResponse {

    private Long id;
    private Long userId;
    private String name;
    private String city;
    private String country;

    public static DashboardPartnerResponse fromEntity(
            TravelPartner partner,
            Long currentUserId
    ) {

        User otherUser =
                partner.getUserOne() != null
                        && partner.getUserOne()
                        .getId()
                        .equals(currentUserId)
                        ? partner.getUserTwo()
                        : partner.getUserOne();

        if (otherUser == null) {
            return new DashboardPartnerResponse(
                    partner.getId(),
                    null,
                    "Travel Partner",
                    null,
                    null
            );
        }

        return new DashboardPartnerResponse(
                partner.getId(),
                otherUser.getId(),
                otherUser.getName(),
                otherUser.getCity(),
                otherUser.getCountry()
        );
    }
}