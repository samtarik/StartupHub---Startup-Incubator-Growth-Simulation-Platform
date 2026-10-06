package com.startuphub.service;

import com.startuphub.model.Notification;
import java.util.List;

public interface NotificationService {
    List<Notification> findByUser(Long userId);
    long countUnread(Long userId);
    void markRead(Long notificationId, Long userId);
    void markAllRead(Long userId);
}
