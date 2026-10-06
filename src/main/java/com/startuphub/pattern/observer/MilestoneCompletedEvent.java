package com.startuphub.pattern.observer;

import com.startuphub.model.Milestone;
import org.springframework.context.ApplicationEvent;

public class MilestoneCompletedEvent extends ApplicationEvent {
    private final Milestone milestone;
    public MilestoneCompletedEvent(Object source, Milestone milestone) {
        super(source);
        this.milestone = milestone;
    }
    public Milestone getMilestone() { return milestone; }
}
