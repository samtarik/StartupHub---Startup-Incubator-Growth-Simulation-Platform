package com.startuphub.pattern.factory;

import com.startuphub.model.Notification;
import com.startuphub.model.User;

public class NotificationFactory {

    public enum NotificationKind {
        FUNDING_RECEIVED, MILESTONE_COMPLETED, CAMPAIGN_LAUNCHED, EMPLOYEE_HIRED,
        MVP_FEATURE_COMPLETED, MVP_LAUNCHED
    }

    public static Notification create(NotificationKind kind, User recipient, String detail) {
        String message = switch (kind) {
            case FUNDING_RECEIVED -> "New investment received: " + detail;
            case MILESTONE_COMPLETED -> "Milestone completed: " + detail;
            case CAMPAIGN_LAUNCHED -> "Marketing campaign launched: " + detail;
            case EMPLOYEE_HIRED -> "New team member hired: " + detail;
            case MVP_FEATURE_COMPLETED -> "MVP feature completed: " + detail;
            case MVP_LAUNCHED -> "MVP launched: " + detail;
        };
        return new Notification(recipient, message);
    }
}
