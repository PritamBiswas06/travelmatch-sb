package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.PushSubscriptionRequest;
import com.pvp.travelmatch.entity.PushSubscription;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.PushSubscriptionRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void subscribe(PushSubscriptionRequest request) {

        if (request.getEndpoint() == null || request.getEndpoint().isBlank()
                || request.getKeys() == null
                || request.getKeys().getP256dh() == null || request.getKeys().getP256dh().isBlank()
                || request.getKeys().getAuth() == null || request.getKeys().getAuth().isBlank()) {
            throw new RuntimeException("Invalid push subscription");
        }

        User currentUser = getCurrentUser();

        Optional<PushSubscription> existing =
                pushSubscriptionRepository.findByEndpoint(request.getEndpoint());

        PushSubscription subscription = existing.orElseGet(() ->
                PushSubscription.builder()
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        subscription.setUser(currentUser);
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getKeys().getP256dh());
        subscription.setAuth(request.getKeys().getAuth());
        subscription.setUpdatedAt(LocalDateTime.now());

        pushSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String endpoint) {

        if (endpoint == null || endpoint.isBlank()) {
            return;
        }

        User currentUser = getCurrentUser();

        pushSubscriptionRepository
                .findByEndpointAndUserId(endpoint, currentUser.getId())
                .ifPresent(pushSubscriptionRepository::delete);
    }
}