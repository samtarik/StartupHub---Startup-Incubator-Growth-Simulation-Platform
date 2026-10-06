package com.startuphub.dto;

import com.startuphub.model.Roadmap;
import java.util.List;

public record RoadmapDto(Long id, String title, List<MilestoneDto> milestones) {
    public static RoadmapDto from(Roadmap r) {
        List<MilestoneDto> ms = r.getMilestones().stream().map(MilestoneDto::from).toList();
        return new RoadmapDto(r.getId(), r.getTitle(), ms);
    }
}
