package com.startuphub.dto;

import com.startuphub.model.Mvp;
import com.startuphub.model.MvpStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** progress / completedFeatures / totalFeatures are computed by the server from the features. */
public record MvpResponse(Long id, Long startupId, String name, String description, String version,
                          MvpStatus status, String targetUsers, LocalDate launchDate,
                          int progress, int completedFeatures, int totalFeatures,
                          LocalDateTime createdAt, LocalDateTime updatedAt,
                          List<MvpFeatureResponse> features) {
    public static MvpResponse from(Mvp m) {
        return new MvpResponse(m.getId(), m.getStartup().getId(), m.getName(), m.getDescription(),
                m.getVersion(), m.getStatus(), m.getTargetUsers(), m.getLaunchDate(),
                m.progressPercent(), m.completedFeatures(), m.totalFeatures(),
                m.getCreatedAt(), m.getUpdatedAt(),
                m.getFeatures().stream().map(MvpFeatureResponse::from).toList());
    }
}
