package com.startuphub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "competitors")
public class Competitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private double marketShare;

    @Column(length = 500)
    private String strengths;

    @Column(length = 500)
    private String weaknesses;

    @Column(length = 500)
    private String opportunities;

    @Column(length = 500)
    private String threats;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    private Startup startup;

    public Competitor() {}

    public Competitor(String name, double marketShare, String strengths, String weaknesses, Startup startup) {
        this.name = name;
        this.marketShare = marketShare;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.startup = startup;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getMarketShare() { return marketShare; }
    public void setMarketShare(double marketShare) { this.marketShare = marketShare; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }
    public String getOpportunities() { return opportunities; }
    public void setOpportunities(String opportunities) { this.opportunities = opportunities; }
    public String getThreats() { return threats; }
    public void setThreats(String threats) { this.threats = threats; }
    public Startup getStartup() { return startup; }
    public void setStartup(Startup startup) { this.startup = startup; }
}
