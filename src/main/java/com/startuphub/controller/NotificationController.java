package com.startuphub.controller;

import com.startuphub.dto.NotificationDto;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return notificationService.findByUser(principal.getUser().getId()).stream().map(NotificationDto::from).toList();
    }

    /** Drives the unread count on the navbar bell. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return Map.of("count", notificationService.countUnread(principal.getUser().getId()));
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markRead(id, principal.getUser().getId());
    }

    @PostMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAllRead(principal.getUser().getId());
    }
}
