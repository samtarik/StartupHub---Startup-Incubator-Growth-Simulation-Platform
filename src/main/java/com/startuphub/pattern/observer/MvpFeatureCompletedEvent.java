package com.startuphub.pattern.observer;

import com.startuphub.model.MvpFeature;
import org.springframework.context.ApplicationEvent;

public class MvpFeatureCompletedEvent extends ApplicationEvent {
    private final MvpFeature feature;
    public MvpFeatureCompletedEvent(Object source, MvpFeature feature) {
        super(source);
        this.feature = feature;
    }
    public MvpFeature getFeature() { return feature; }
}
