package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FriendResponse {
    private Long userId;
    private String name;
    private String username;
    private String city;
    private String country;
    private String gender;
    private String profilePhotoUrl;
}
