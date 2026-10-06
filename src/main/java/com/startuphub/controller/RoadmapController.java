package com.startuphub.controller;

import com.startuphub.dto.MilestoneEditRequest;
import com.startuphub.dto.MilestoneDto;
import com.startuphub.dto.MilestoneRequest;
import com.startuphub.dto.MilestoneUpdateRequest;
import com.startuphub.dto.RoadmapDto;
import com.startuphub.dto.RoadmapRequest;
import com.startuphub.security.AccessGuard;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.RoadmapService;
import com.startuphub.service.StartupService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class RoadmapController {
    private final RoadmapService roadmapService;
    private final StartupService startupService;
    private final AccessGuard accessGuard;

    public RoadmapController(RoadmapService roadmapService, StartupService startupService, AccessGuard accessGuard) {
        this.roadmapService = roadmapService;
        this.startupService = startupService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/startups/{startupId}/roadmaps")
    public List<RoadmapDto> list(@PathVariable Long startupId) {
        return roadmapService.findByStartup(startupId).stream().map(RoadmapDto::from).toList();
    }

    @PostMapping("/api/startups/{startupId}/roadmaps")
    public RoadmapDto create(@PathVariable Long startupId, @Valid @RequestBody RoadmapRequest req,
                             @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        return RoadmapDto.from(roadmapService.createRoadmap(req.title(), startup));
    }

    @PostMapping("/api/roadmaps/{roadmapId}/milestones")
    public MilestoneDto addMilestone(@PathVariable Long roadmapId, @Valid @RequestBody MilestoneRequest req,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        var roadmap = roadmapService.findRoadmap(roadmapId);
        accessGuard.requireOwner(principal.getUser(), roadmap.getStartup());
        return MilestoneDto.from(roadmapService.addMilestone(roadmapId, req.title(), req.description(),
                req.deadline(), req.priority()));
    }

    @PostMapping("/api/milestones/{milestoneId}/complete")
    public MilestoneDto complete(@PathVariable Long milestoneId, @AuthenticationPrincipal CustomUserDetails principal) {
        var milestone = roadmapService.findMilestone(milestoneId);
        accessGuard.requireOwnerOrMentor(principal.getUser(), milestone.getRoadmap().getStartup());
        return MilestoneDto.from(roadmapService.completeMilestone(milestoneId));
    }

    /** Moves a milestone to IN_PROGRESS / DELAYED / PENDING, or nudges its progress bar. */
    @PostMapping("/api/milestones/{milestoneId}/status")
    public MilestoneDto updateStatus(@PathVariable Long milestoneId,
                                     @Valid @RequestBody MilestoneUpdateRequest req,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        var milestone = roadmapService.findMilestone(milestoneId);
        accessGuard.requireOwnerOrMentor(principal.getUser(), milestone.getRoadmap().getStartup());
        return MilestoneDto.from(roadmapService.updateMilestone(milestoneId, req.status(), req.progress()));
    }

    @PutMapping("/api/roadmaps/{roadmapId}")
    public RoadmapDto updateRoadmap(@PathVariable Long roadmapId, @Valid @RequestBody RoadmapRequest req,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        var roadmap = roadmapService.findRoadmap(roadmapId);
        accessGuard.requireOwner(principal.getUser(), roadmap.getStartup());
        return RoadmapDto.from(roadmapService.updateRoadmap(roadmapId, req.title()));
    }

    @DeleteMapping("/api/roadmaps/{roadmapId}")
    public void deleteRoadmap(@PathVariable Long roadmapId, @AuthenticationPrincipal CustomUserDetails principal) {
        var roadmap = roadmapService.findRoadmap(roadmapId);
        accessGuard.requireOwner(principal.getUser(), roadmap.getStartup());
        roadmapService.deleteRoadmap(roadmapId);
    }

    /** Full milestone edit (title, description, deadline, priority, optionally status/progress). */
    @PutMapping("/api/milestones/{milestoneId}")
    public MilestoneDto editMilestone(@PathVariable Long milestoneId, @Valid @RequestBody MilestoneEditRequest req,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        var milestone = roadmapService.findMilestone(milestoneId);
        accessGuard.requireOwner(principal.getUser(), milestone.getRoadmap().getStartup());
        return MilestoneDto.from(roadmapService.editMilestone(milestoneId, req.title(), req.description(),
                req.deadline(), req.priority(), req.status(), req.progress()));
    }

    @DeleteMapping("/api/milestones/{milestoneId}")
    public void deleteMilestone(@PathVariable Long milestoneId, @AuthenticationPrincipal CustomUserDetails principal) {
        var milestone = roadmapService.findMilestone(milestoneId);
        accessGuard.requireOwner(principal.getUser(), milestone.getRoadmap().getStartup());
        roadmapService.deleteMilestone(milestoneId);
    }
}
