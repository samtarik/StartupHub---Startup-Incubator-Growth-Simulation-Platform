package com.startuphub.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "revenues")
public class Revenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double amount;
    private String source;
    private LocalDate recordedOn = LocalDate.now();

    /** Set only when the amount came from the automatic revenue calculator (null = entered manually). */
    @Enumerated(EnumType.STRING)
    private RevenueModel revenueModel;

    /** Human-readable formula that produced the amount, e.g. "100 customers x $20.00/month = $2,000.00". */
    @Column(length = 500)
    private String calculationDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    private Startup startup;

    public Revenue() {}

    public Revenue(double amount, String source, Startup startup) {
        this.amount = amount;
        this.source = source;
        this.startup = startup;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDate getRecordedOn() { return recordedOn; }
    public void setRecordedOn(LocalDate recordedOn) { this.recordedOn = recordedOn; }
    public RevenueModel getRevenueModel() { return revenueModel; }
    public void setRevenueModel(RevenueModel revenueModel) { this.revenueModel = revenueModel; }
    public String getCalculationDetails() { return calculationDetails; }
    public void setCalculationDetails(String calculationDetails) { this.calculationDetails = calculationDetails; }
    public Startup getStartup() { return startup; }
    public void setStartup(Startup startup) { this.startup = startup; }
}
