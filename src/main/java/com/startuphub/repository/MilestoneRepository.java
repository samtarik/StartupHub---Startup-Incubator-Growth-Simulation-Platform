package com.startuphub.repository;

import com.startuphub.model.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByRoadmapId(Long roadmapId);
}
