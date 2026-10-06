package com.startuphub.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The Minimum Viable Product a startup is building. This is separate from the MVP
 * lifecycle STAGE (IDEA -> MVP -> GROWTH -> SCALE): the stage says where the company is,
 * this entity tracks the actual product, its version, and the features that make it up.
 *
 * One MVP per startup. Progress is never stored - it is derived from the features.
 */
@Entity
@Table(name = "mvp")
public class Mvp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false, unique = true)
    private Startup startup;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String version = "1.0";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MvpStatus status = MvpStatus.PLANNING;

    private String targetUsers;
    private LocalDate launchDate;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "mvp", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MvpFeature> features = new ArrayList<>();

    public Mvp() {}

    public Mvp(Startup startup, String name, String description, String version,
               MvpStatus status, String targetUsers, LocalDate launchDate) {
        this.startup = startup;
        this.name = name;
        this.description = description;
        this.version = version;
        this.status = status == null ? MvpStatus.PLANNING : status;
        this.targetUsers = targetUsers;
        this.launchDate = launchDate;
    }

    public int totalFeatures() { return features.size(); }

    public int completedFeatures() {
        int done = 0;
        for (MvpFeature f : features) if (f.isCompleted()) done++;
        return done;
    }

    /**
     * MVP progress = completed features / total features x 100, rounded to a whole percent.
     * Zero features is a valid state and gives 0 rather than dividing by zero.
     */
    public int progressPercent() {
        int total = totalFeatures();
        if (total == 0) return 0;
        return (int) Math.round(completedFeatures() * 100.0 / total);
    }

    @PreUpdate
    void touch() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Startup getStartup() { return startup; }
    public void setStartup(Startup startup) { this.startup = startup; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public MvpStatus getStatus() { return status; }
    public void setStatus(MvpStatus status) { this.status = status; }
    public String getTargetUsers() { return targetUsers; }
    public void setTargetUsers(String targetUsers) { this.targetUsers = targetUsers; }
    public LocalDate getLaunchDate() { return launchDate; }
    public void setLaunchDate(LocalDate launchDate) { this.launchDate = launchDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<MvpFeature> getFeatures() { return features; }
    public void setFeatures(List<MvpFeature> features) { this.features = features; }
}
