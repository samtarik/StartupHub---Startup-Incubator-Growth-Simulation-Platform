package com.startuphub.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "investments")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    private Startup startup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id", nullable = false)
    private User investor;

    @Enumerated(EnumType.STRING)
    private InvestmentType type;

    private double amount;
    private LocalDate investedOn = LocalDate.now();

    public Investment() {}

    public Investment(Startup startup, User investor, InvestmentType type, double amount) {
        this.startup = startup;
        this.investor = investor;
        this.type = type;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Startup getStartup() { return startup; }
    public void setStartup(Startup startup) { this.startup = startup; }
    public User getInvestor() { return investor; }
    public void setInvestor(User investor) { this.investor = investor; }
    public InvestmentType getType() { return type; }
    public void setType(InvestmentType type) { this.type = type; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDate getInvestedOn() { return investedOn; }
    public void setInvestedOn(LocalDate investedOn) { this.investedOn = investedOn; }
}
