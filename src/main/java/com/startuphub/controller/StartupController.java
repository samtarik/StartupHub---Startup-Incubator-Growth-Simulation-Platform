package com.startuphub.controller;

import com.startuphub.dto.StartupUpdateRequest;
import com.startuphub.dto.StartupDto;
import com.startuphub.dto.StartupRequest;
import com.startuphub.security.AccessGuard;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.StartupService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/startups")
public class StartupController {
    private final StartupService startupService;
    private final AccessGuard accessGuard;

    public StartupController(StartupService startupService, AccessGuard accessGuard) {
        this.startupService = startupService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<StartupDto> all() {
        return startupService.findAll().stream().map(StartupDto::from).toList();
    }

    @GetMapping("/mine")
    public List<StartupDto> mine(@AuthenticationPrincipal CustomUserDetails principal) {
        return startupService.findByFounder(principal.getUser().getId()).stream().map(StartupDto::from).toList();
    }

    @PostMapping
    public StartupDto create(@Valid @RequestBody StartupRequest req, @AuthenticationPrincipal CustomUserDetails principal) {
        return StartupDto.from(startupService.create(req.name(), req.description(), principal.getUser()));
    }

    @GetMapping("/{id}")
    public StartupDto one(@PathVariable Long id) {
        return StartupDto.from(startupService.findById(id));
    }

    @PostMapping("/{id}/advance-stage")
    public StartupDto advanceStage(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(id);
        accessGuard.requireOwner(principal.getUser(), startup);
        return StartupDto.from(startupService.advanceStage(id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(id);
        accessGuard.requireOwner(principal.getUser(), startup);
        startupService.delete(id);
    }

    @PutMapping("/{id}")
    public StartupDto update(@PathVariable Long id, @Valid @RequestBody StartupUpdateRequest req,
                             @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(id);
        accessGuard.requireOwner(principal.getUser(), startup);
        return StartupDto.from(startupService.update(id, req.name(), req.description(), req.valuation()));
    }
}
