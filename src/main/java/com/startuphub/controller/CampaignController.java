package com.startuphub.controller;

import com.startuphub.dto.CampaignUpdateRequest;
import com.startuphub.dto.CampaignDto;
import com.startuphub.dto.CampaignMetricsRequest;
import com.startuphub.dto.CampaignRequest;
import com.startuphub.security.AccessGuard;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.CampaignService;
import com.startuphub.service.StartupService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class CampaignController {
    private final CampaignService campaignService;
    private final StartupService startupService;
    private final AccessGuard accessGuard;

    public CampaignController(CampaignService campaignService, StartupService startupService, AccessGuard accessGuard) {
        this.campaignService = campaignService;
        this.startupService = startupService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/startups/{startupId}/campaigns")
    public List<CampaignDto> list(@PathVariable Long startupId) {
        return campaignService.findByStartup(startupId).stream().map(CampaignDto::from).toList();
    }

    @PostMapping("/api/startups/{startupId}/campaigns")
    public CampaignDto launch(@PathVariable Long startupId, @Valid @RequestBody CampaignRequest req,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        return CampaignDto.from(campaignService.launch(req.name(), req.budget(), startup));
    }

    /** Records how the campaign actually performed, so the bar chart plots real numbers. */
    @PostMapping("/api/campaigns/{campaignId}/metrics")
    public CampaignDto recordMetrics(@PathVariable Long campaignId, @Valid @RequestBody CampaignMetricsRequest req,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        var campaign = campaignService.findById(campaignId);
        accessGuard.requireOwner(principal.getUser(), campaign.getStartup());
        return CampaignDto.from(campaignService.updateMetrics(campaignId, req.reach(), req.conversions()));
    }

    @PutMapping("/api/campaigns/{campaignId}")
    public CampaignDto update(@PathVariable Long campaignId, @Valid @RequestBody CampaignUpdateRequest req,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        var campaign = campaignService.findById(campaignId);
        accessGuard.requireOwner(principal.getUser(), campaign.getStartup());
        return CampaignDto.from(campaignService.update(campaignId, req.name(), req.budget(),
                req.reach(), req.conversions()));
    }

    @DeleteMapping("/api/campaigns/{campaignId}")
    public void delete(@PathVariable Long campaignId, @AuthenticationPrincipal CustomUserDetails principal) {
        var campaign = campaignService.findById(campaignId);
        accessGuard.requireOwner(principal.getUser(), campaign.getStartup());
        campaignService.delete(campaignId);
    }
}
