package com.startuphub.service;

import com.startuphub.dto.HealthDto;
import java.util.Map;

public interface AnalyticsService {
    Map<String, Object> summarize(Long startupId);

    /** Startup Health Score (0-100), computed from finance, team, roadmap, funding and growth. */
    HealthDto health(Long startupId);
}
