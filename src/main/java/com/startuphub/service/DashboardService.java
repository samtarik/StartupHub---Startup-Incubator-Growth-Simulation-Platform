package com.startuphub.service;

import com.startuphub.dto.ActivityDto;
import com.startuphub.model.User;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the numbers the dashboard KPI cards, the trend chart and the activity
 * feed need, so the frontend makes one call instead of one call per startup.
 */
public interface DashboardService {
    Map<String, Object> summaryFor(User user);
    List<ActivityDto> activityFor(User user, int limit);
}
