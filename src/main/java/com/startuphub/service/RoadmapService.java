package com.startuphub.service;

import com.startuphub.model.Milestone;
import com.startuphub.model.MilestonePriority;
import com.startuphub.model.MilestoneStatus;
import com.startuphub.model.Roadmap;
import com.startuphub.model.Startup;
import java.time.LocalDate;
import java.util.List;

public interface RoadmapService {
    Roadmap createRoadmap(String title, Startup startup);
    List<Roadmap> findByStartup(Long startupId);
    Roadmap findRoadmap(Long roadmapId);
    Milestone findMilestone(Long milestoneId);
    Milestone addMilestone(Long roadmapId, String title, String description,
                           LocalDate deadline, MilestonePriority priority);
    Milestone completeMilestone(Long milestoneId);
    Milestone updateMilestone(Long milestoneId, MilestoneStatus status, Integer progress);
    Roadmap updateRoadmap(Long roadmapId, String title);
    void deleteRoadmap(Long roadmapId);
    Milestone editMilestone(Long milestoneId, String title, String description, LocalDate deadline,
                            MilestonePriority priority, MilestoneStatus status, Integer progress);
    void deleteMilestone(Long milestoneId);
}
