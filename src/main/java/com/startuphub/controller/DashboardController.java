package com.startuphub.controller;

import com.startuphub.dto.ActivityDto;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feeds the redesigned dashboard: KPI cards, the revenue/expense trend chart
 * and the Recent Activity feed - all scoped to the logged-in user's role.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@AuthenticationPrincipal CustomUserDetails principal) {
        return dashboardService.summaryFor(principal.getUser());
    }

    @GetMapping("/activity")
    public List<ActivityDto> activity(@AuthenticationPrincipal CustomUserDetails principal,
                                      @RequestParam(defaultValue = "12") int limit) {
        return dashboardService.activityFor(principal.getUser(), Math.min(Math.max(limit, 1), 50));
    }
}
