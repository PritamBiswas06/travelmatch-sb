package com.pvp.travelmatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvp.travelmatch.entity.NotificationType;
import com.pvp.travelmatch.entity.PushSubscription;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

//import java.net.http.HttpResponse;
import java.security.Security;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebPushService {

    @Value("${vapid.public.key}")
    private String vapidPublicKey;

    @Value("${vapid.private.key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper;

    private PushService pushService;

    @PostConstruct
    private void init() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        } catch (Exception e) {
            System.err.println("Web Push disabled: failed to initialize VAPID keys");
            e.printStackTrace();
            pushService = null;
        }
    }

    public String getPublicKey() {
        return vapidPublicKey;
    }

    public void sendToUser(User receiver, String title, String message,
                           NotificationType type, Long relatedEntityId) {

        if (pushService == null) {
            return;
        }

        List<PushSubscription> subscriptions =
                pushSubscriptionRepository.findByUserId(receiver.getId());

        if (subscriptions.isEmpty()) {
            return;
        }

        String payload = buildPayload(title, message, type, relatedEntityId);

        for (PushSubscription subscription : subscriptions) {
            sendToSubscription(subscription, payload);
        }
    }

    private void sendToSubscription(PushSubscription subscription, String payload) {
        try {
            Subscription sub = new Subscription(
                    subscription.getEndpoint(),
                    new Subscription.Keys(subscription.getP256dh(), subscription.getAuth())
            );

            HttpResponse response = pushService.send(new Notification(sub, payload));

            int status = response.getStatusLine().getStatusCode();

            System.out.println(
                    "WEB PUSH RESULT: subscription=" + subscription.getId()
                            + ", status=" + status
            );

            if (status == 404 || status == 410) {
                pushSubscriptionRepository.delete(subscription);
            } else if (status < 200 || status >= 300) {
                System.err.println("Web Push delivery failed with status " + status
                        + " for subscription " + subscription.getId());
            }

        } catch (Exception e) {
            System.err.println("Web Push delivery failed for subscription " + subscription.getId());
            e.printStackTrace();
        }
    }

    private String buildPayload(String title, String message, NotificationType type, Long relatedEntityId) {

        String url = "/notifications";

        if (type == NotificationType.MATCH_REQUEST_RECEIVED
                || type == NotificationType.MATCH_REQUEST_ACCEPTED
                || type == NotificationType.MATCH_REQUEST_REJECTED) {
            url = "/requests";
        } else if (type == NotificationType.PROFILE_VIEW && relatedEntityId != null) {
            url = "/profile/" + relatedEntityId;
        } else if (type == NotificationType.NEW_MESSAGE && relatedEntityId != null) {
            url = "/chat/" + relatedEntityId;
        } else if (type == NotificationType.POST_LIKE) {
            url = "/feed";
        }

        Map<String, Object> onActionClick = Map.of(
                "default", Map.of("operation", "navigateLastFocusedOrOpen", "url", url)
        );

        Map<String, Object> notification = Map.of(
                "title", title,
                "body", message,
                "icon", "/TravelMatchLogo.png",
                "data", Map.of("onActionClick", onActionClick)
        );

        try {
            return objectMapper.writeValueAsString(Map.of("notification", notification));
        } catch (Exception e) {
            return "{\"notification\":{\"title\":\"" + title + "\",\"body\":\"" + message + "\"}}";
        }
    }
}