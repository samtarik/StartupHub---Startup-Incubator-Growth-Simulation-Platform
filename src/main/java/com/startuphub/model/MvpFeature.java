package com.startuphub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One requirement/feature of an MVP. Reuses MilestonePriority (LOW / MEDIUM / HIGH). */
@Entity
@Table(name = "mvp_feature")
public class MvpFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mvp_id", nullable = false)
    private Mvp mvp;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    private boolean completed = false;

    @Enumerated(EnumType.STRING)
    private MilestonePriority priority = MilestonePriority.MEDIUM;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    public MvpFeature() {}

    public MvpFeature(Mvp mvp, String title, String description, MilestonePriority priority) {
        this.mvp = mvp;
        this.title = title;
        this.description = description;
        this.priority = priority == null ? MilestonePriority.MEDIUM : priority;
    }

    /** Single place where completion changes, so completedAt always matches the flag. */
    public void markCompleted(boolean value) {
        this.completed = value;
        this.completedAt = value ? LocalDateTime.now() : null;
    }

    @PreUpdate
    void touch() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Mvp getMvp() { return mvp; }
    public void setMvp(Mvp mvp) { this.mvp = mvp; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public MilestonePriority getPriority() { return priority; }
    public void setPriority(MilestonePriority priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
