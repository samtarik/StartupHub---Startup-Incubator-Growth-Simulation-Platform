package com.startuphub.pattern.observer;

import com.startuphub.dto.NotificationDto;
import com.startuphub.model.Investment;
import com.startuphub.model.Milestone;
import com.startuphub.model.Mvp;
import com.startuphub.model.MvpFeature;
import com.startuphub.model.Notification;
import com.startuphub.pattern.factory.NotificationFactory;
import com.startuphub.repository.NotificationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Observer pattern: reacts to domain events published elsewhere via
 * ApplicationEventPublisher, without the publisher knowing about
 * notifications or WebSocket delivery at all.
 */
@Component
public class StartupEventListener {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public StartupEventListener(NotificationRepository notificationRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onInvestmentReceived(InvestmentReceivedEvent event) {
        Investment inv = event.getInvestment();
        Notification n = NotificationFactory.create(
                NotificationFactory.NotificationKind.FUNDING_RECEIVED,
                inv.getStartup().getFounder(),
                inv.getStartup().getName() + " received $" + inv.getAmount() + " from " + inv.getInvestor().getName());
        n = notificationRepository.save(n);
        push(n);
    }

    @EventListener
    public void onMilestoneCompleted(MilestoneCompletedEvent event) {
        Milestone m = event.getMilestone();
        Notification n = NotificationFactory.create(
                NotificationFactory.NotificationKind.MILESTONE_COMPLETED,
                m.getRoadmap().getStartup().getFounder(),
                m.getTitle());
        n = notificationRepository.save(n);
        push(n);
    }

    @EventListener
    public void onMvpFeatureCompleted(MvpFeatureCompletedEvent event) {
        MvpFeature f = event.getFeature();
        Mvp mvp = f.getMvp();
        Notification n = NotificationFactory.create(
                NotificationFactory.NotificationKind.MVP_FEATURE_COMPLETED,
                mvp.getStartup().getFounder(),
                f.getTitle() + " (" + mvp.progressPercent() + "% of MVP " + mvp.getVersion() + " done)");
        n = notificationRepository.save(n);
        push(n);
    }

    @EventListener
    public void onMvpLaunched(MvpLaunchedEvent event) {
        Mvp mvp = event.getMvp();
        Notification n = NotificationFactory.create(
                NotificationFactory.NotificationKind.MVP_LAUNCHED,
                mvp.getStartup().getFounder(),
                mvp.getStartup().getName() + " MVP " + mvp.getVersion() + " is live");
        n = notificationRepository.save(n);
        push(n);
    }

    private void push(Notification n) {
        // Live-push over WebSocket/STOMP to the recipient's private notification queue.
        messagingTemplate.convertAndSend(
                "/topic/notifications-" + n.getUser().getId(),
                NotificationDto.from(n));
    }
}
