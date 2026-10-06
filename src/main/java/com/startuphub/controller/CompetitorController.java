package com.startuphub.controller;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Competitor;
import com.startuphub.dto.CompetitorDto;
import com.startuphub.dto.CompetitorRequest;
import com.startuphub.security.AccessGuard;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.CompetitorService;
import com.startuphub.service.StartupService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/startups/{startupId}/competitors")
public class CompetitorController {
    private final CompetitorService competitorService;
    private final StartupService startupService;
    private final AccessGuard accessGuard;

    public CompetitorController(CompetitorService competitorService, StartupService startupService,
                                AccessGuard accessGuard) {
        this.competitorService = competitorService;
        this.startupService = startupService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<CompetitorDto> list(@PathVariable Long startupId) {
        return competitorService.findByStartup(startupId).stream().map(CompetitorDto::from).toList();
    }

    @PostMapping
    public CompetitorDto add(@PathVariable Long startupId, @Valid @RequestBody CompetitorRequest req,
                             @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        return CompetitorDto.from(competitorService.add(req.name(), req.marketShare(),
                req.strengths(), req.weaknesses(), req.opportunities(), req.threats(), startup));
    }

    @PutMapping("/{competitorId}")
    public CompetitorDto update(@PathVariable Long startupId, @PathVariable Long competitorId,
                                @Valid @RequestBody CompetitorRequest req,
                                @AuthenticationPrincipal CustomUserDetails principal) {
        var competitor = ownedCompetitor(startupId, competitorId, principal);
        return CompetitorDto.from(competitorService.update(competitor.getId(), req.name(), req.marketShare(),
                req.strengths(), req.weaknesses(), req.opportunities(), req.threats()));
    }

    @DeleteMapping("/{competitorId}")
    public void delete(@PathVariable Long startupId, @PathVariable Long competitorId,
                       @AuthenticationPrincipal CustomUserDetails principal) {
        var competitor = ownedCompetitor(startupId, competitorId, principal);
        competitorService.delete(competitor.getId());
    }

    /** Loads the competitor, makes sure it belongs to the startup in the URL, and checks ownership. */
    private Competitor ownedCompetitor(Long startupId, Long competitorId, CustomUserDetails principal) {
        Competitor competitor = competitorService.findById(competitorId);
        if (!competitor.getStartup().getId().equals(startupId)) {
            throw new ResourceNotFoundException("Competitor not found: " + competitorId);
        }
        accessGuard.requireOwner(principal.getUser(), competitor.getStartup());
        return competitor;
    }
}
