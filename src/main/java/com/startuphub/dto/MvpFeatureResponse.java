package com.startuphub.dto;

import com.startuphub.model.MilestonePriority;
import com.startuphub.model.MvpFeature;
import java.time.LocalDateTime;

public record MvpFeatureResponse(Long id, String title, String description, boolean completed,
                                 MilestonePriority priority, LocalDateTime createdAt,
                                 LocalDateTime updatedAt, LocalDateTime completedAt) {
    public static MvpFeatureResponse from(MvpFeature f) {
        return new MvpFeatureResponse(f.getId(), f.getTitle(), f.getDescription(), f.isCompleted(),
                f.getPriority(), f.getCreatedAt(), f.getUpdatedAt(), f.getCompletedAt());
    }
}
