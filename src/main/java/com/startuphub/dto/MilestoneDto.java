package com.startuphub.dto;

import com.startuphub.model.Milestone;
import com.startuphub.model.MilestonePriority;
import com.startuphub.model.MilestoneStatus;
import java.time.LocalDate;

public record MilestoneDto(Long id, String title, String description, LocalDate deadline,
                           MilestoneStatus status, MilestonePriority priority,
                           int progress, boolean overdue) {
    public static MilestoneDto from(Milestone m) {
        return new MilestoneDto(m.getId(), m.getTitle(), m.getDescription(), m.getDeadline(),
                m.getStatus(), m.getPriority(), m.getProgress(), m.isOverdue());
    }
}
