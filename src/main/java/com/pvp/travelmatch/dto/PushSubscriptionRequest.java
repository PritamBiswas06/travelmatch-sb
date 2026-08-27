package com.pvp.travelmatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionRequest {

    private String endpoint;
    private Keys keys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}