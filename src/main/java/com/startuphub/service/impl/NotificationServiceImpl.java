package com.startuphub.service.impl;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Notification;
import com.startuphub.repository.NotificationRepository;
import com.startuphub.service.NotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<Notification> findByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return findByUser(userId).stream().filter(n -> !n.isRead()).count();
    }

    /** A user can only mark their own notifications as read. */
    @Override
    public void markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (!n.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("That notification does not belong to you.");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Override
    public void markAllRead(Long userId) {
        List<Notification> unread = findByUser(userId).stream().filter(n -> !n.isRead()).toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
