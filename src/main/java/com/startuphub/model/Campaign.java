package com.startuphub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private double budget;
    private long reach;
    private long conversions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    private Startup startup;

    public Campaign() {}

    public Campaign(String name, double budget, Startup startup) {
        this.name = name;
        this.budget = budget;
        this.startup = startup;
    }

    public double conversionRate() {
        return reach == 0 ? 0 : (conversions * 100.0) / reach;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }
    public long getReach() { return reach; }
    public void setReach(long reach) { this.reach = reach; }
    public long getConversions() { return conversions; }
    public void setConversions(long conversions) { this.conversions = conversions; }
    public Startup getStartup() { return startup; }
    public void setStartup(Startup startup) { this.startup = startup; }
}
