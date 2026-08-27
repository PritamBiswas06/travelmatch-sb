package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.PushSubscriptionRequest;
import com.pvp.travelmatch.service.PushSubscriptionService;
import com.pvp.travelmatch.service.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;
    private final WebPushService webPushService;

    @GetMapping("/public-key")
    public Map<String, String> getPublicKey() {
        return Map.of("publicKey", webPushService.getPublicKey());
    }

    @PostMapping("/subscribe")
    public Map<String, String> subscribe(@RequestBody PushSubscriptionRequest request) {
        pushSubscriptionService.subscribe(request);
        return Map.of("message", "Subscribed to push notifications");
    }

    @PostMapping("/unsubscribe")
    public Map<String, String> unsubscribe(@RequestBody Map<String, String> body) {
        pushSubscriptionService.unsubscribe(body.get("endpoint"));
        return Map.of("message", "Unsubscribed from push notifications");
    }
}