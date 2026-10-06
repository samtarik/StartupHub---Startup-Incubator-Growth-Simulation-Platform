package com.startuphub.dto;

import com.startuphub.model.Notification;
import java.time.LocalDateTime;

public record NotificationDto(Long id, String message, boolean read, LocalDateTime createdAt) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
