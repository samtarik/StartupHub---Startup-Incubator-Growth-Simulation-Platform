package com.startuphub.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "milestones")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private MilestoneStatus status = MilestoneStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private MilestonePriority priority = MilestonePriority.MEDIUM;

    /** 0-100 completion percentage, drives the roadmap timeline progress bars. */
    private int progress = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private Roadmap roadmap;

    public Milestone() {}

    public Milestone(String title, LocalDate deadline, Roadmap roadmap) {
        this.title = title;
        this.deadline = deadline;
        this.roadmap = roadmap;
    }

    public Milestone(String title, String description, LocalDate deadline,
                     MilestonePriority priority, Roadmap roadmap) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority == null ? MilestonePriority.MEDIUM : priority;
        this.roadmap = roadmap;
    }

    /** Derived, never stored: past its deadline and not finished. */
    public boolean isOverdue() {
        return deadline != null && status != MilestoneStatus.DONE && deadline.isBefore(LocalDate.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public MilestoneStatus getStatus() { return status; }
    public void setStatus(MilestoneStatus status) { this.status = status; }
    public MilestonePriority getPriority() { return priority; }
    public void setPriority(MilestonePriority priority) { this.priority = priority; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = Math.max(0, Math.min(100, progress)); }
    public Roadmap getRoadmap() { return roadmap; }
    public void setRoadmap(Roadmap roadmap) { this.roadmap = roadmap; }
}
