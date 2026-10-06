package com.startuphub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "startups")
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "founder_id", nullable = false)
    private User founder;

    @Enumerated(EnumType.STRING)
    private StartupStage stage = StartupStage.IDEA;

    private double valuation = 0.0;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Startup() {}

    public Startup(String name, String description, User founder) {
        this.name = name;
        this.description = description;
        this.founder = founder;
    }

    public void advanceStage() {
        this.stage = switch (this.stage) {
            case IDEA -> StartupStage.MVP;
            case MVP -> StartupStage.GROWTH;
            case GROWTH -> StartupStage.SCALE;
            case SCALE -> StartupStage.SCALE;
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getFounder() { return founder; }
    public void setFounder(User founder) { this.founder = founder; }
    public StartupStage getStage() { return stage; }
    public void setStage(StartupStage stage) { this.stage = stage; }
    public double getValuation() { return valuation; }
    public void setValuation(double valuation) { this.valuation = valuation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
