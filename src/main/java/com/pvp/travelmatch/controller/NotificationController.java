package com.pvp.travelmatch.controller;

import com.pvp.travelmatch.dto.NotificationResponse;
import com.pvp.travelmatch.dto.UnreadCountResponse;
import com.pvp.travelmatch.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Get all notifications for the logged-in user (newest first)
    @GetMapping
    public List<NotificationResponse> getMyNotifications() {
        return notificationService.getMyNotifications();
    }

    // Unread notification count for the logged-in user
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount() {
        return new UnreadCountResponse(notificationService.getUnreadCount());
    }

    // Mark a single notification as read (must belong to the logged-in user)
    @PutMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    // Mark all notifications as read for the logged-in user
    @PutMapping("/read-all")
    public Map<String, String> markAllAsRead() {
        notificationService.markAllAsRead();
        return Map.of("message", "All notifications marked as read");
    }
}