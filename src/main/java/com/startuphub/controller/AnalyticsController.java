package com.startuphub.controller;

import com.startuphub.dto.HealthDto;
import com.startuphub.dto.ProjectionRequest;
import com.startuphub.service.AnalyticsService;
import com.startuphub.service.FinanceService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/startups/{startupId}/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final FinanceService financeService;

    public AnalyticsController(AnalyticsService analyticsService, FinanceService financeService) {
        this.analyticsService = analyticsService;
        this.financeService = financeService;
    }

    @GetMapping
    public Map<String, Object> summary(@PathVariable Long startupId) {
        return analyticsService.summarize(startupId);
    }

    /** Startup Health Score shown on the Overview tab. */
    @GetMapping("/health")
    public HealthDto health(@PathVariable Long startupId) {
        return analyticsService.health(startupId);
    }

    // Strategy pattern: the strategy name arrives from the frontend and FinanceServiceImpl
    // picks the matching RevenueStrategy bean at runtime.
    @PostMapping("/project")
    public Map<String, Object> project(@PathVariable Long startupId, @RequestBody ProjectionRequest req) {
        double projection = financeService.projectRevenue(req.strategy(), req.baseAmount(), req.customers());
        return Map.of("strategy", req.strategy(), "projection", projection);
    }
}
