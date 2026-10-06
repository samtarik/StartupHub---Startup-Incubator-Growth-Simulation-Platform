package com.startuphub.pattern.observer;

import com.startuphub.model.Mvp;
import org.springframework.context.ApplicationEvent;

public class MvpLaunchedEvent extends ApplicationEvent {
    private final Mvp mvp;
    public MvpLaunchedEvent(Object source, Mvp mvp) {
        super(source);
        this.mvp = mvp;
    }
    public Mvp getMvp() { return mvp; }
}
