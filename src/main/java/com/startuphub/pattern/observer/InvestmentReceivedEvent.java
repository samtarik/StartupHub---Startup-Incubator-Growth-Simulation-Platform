package com.startuphub.pattern.observer;

import com.startuphub.model.Investment;
import org.springframework.context.ApplicationEvent;

public class InvestmentReceivedEvent extends ApplicationEvent {
    private final Investment investment;
    public InvestmentReceivedEvent(Object source, Investment investment) {
        super(source);
        this.investment = investment;
    }
    public Investment getInvestment() { return investment; }
}
