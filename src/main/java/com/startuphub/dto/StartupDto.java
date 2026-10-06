package com.startuphub.dto;

import com.startuphub.model.Startup;
import com.startuphub.model.StartupStage;
import java.time.LocalDateTime;

public record StartupDto(Long id, String name, String description, StartupStage stage,
                          double valuation, Long founderId, String founderName, LocalDateTime createdAt) {
    public static StartupDto from(Startup s) {
        return new StartupDto(s.getId(), s.getName(), s.getDescription(), s.getStage(),
                s.getValuation(), s.getFounder().getId(), s.getFounder().getName(), s.getCreatedAt());
    }
}
