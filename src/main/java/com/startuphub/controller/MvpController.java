package com.startuphub.controller;

import com.startuphub.dto.MvpFeatureRequest;
import com.startuphub.dto.MvpFeatureResponse;
import com.startuphub.dto.MvpRequest;
import com.startuphub.dto.MvpResponse;
import com.startuphub.security.AccessGuard;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.MvpService;
import com.startuphub.service.StartupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * MVP management for a startup. Reading is open to any signed-in user (like every other tab);
 * every change goes through AccessGuard.requireOwner (founder or admin).
 */
@RestController
@RequestMapping("/api/startups/{startupId}/mvp")
public class MvpController {
    private final MvpService mvpService;
    private final StartupService startupService;
    private final AccessGuard accessGuard;

    public MvpController(MvpService mvpService, StartupService startupService, AccessGuard accessGuard) {
        this.mvpService = mvpService;
        this.startupService = startupService;
        this.accessGuard = accessGuard;
    }

    /** 200 with the MVP, or 204 No Content when this startup has not defined one yet. */
    @GetMapping
    public ResponseEntity<MvpResponse> get(@PathVariable Long startupId) {
        startupService.findById(startupId); // 404 for an unknown startup
        return mvpService.findByStartup(startupId)
                .map(m -> ResponseEntity.ok(MvpResponse.from(m)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public MvpResponse create(@PathVariable Long startupId, @Valid @RequestBody MvpRequest req,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        return MvpResponse.from(mvpService.create(startup, req));
    }

    @PutMapping("/{mvpId}")
    public MvpResponse update(@PathVariable Long startupId, @PathVariable Long mvpId,
                              @Valid @RequestBody MvpRequest req,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        requireOwner(startupId, principal);
        return MvpResponse.from(mvpService.update(startupId, mvpId, req));
    }

    @DeleteMapping("/{mvpId}")
    public void delete(@PathVariable Long startupId, @PathVariable Long mvpId,
                       @AuthenticationPrincipal CustomUserDetails principal) {
        requireOwner(startupId, principal);
        mvpService.delete(startupId, mvpId);
    }

    // ---- features ----------------------------------------------------------------

    @PostMapping("/{mvpId}/features")
    public MvpFeatureResponse addFeature(@PathVariable Long startupId, @PathVariable Long mvpId,
                                         @Valid @RequestBody MvpFeatureRequest req,
                                         @AuthenticationPrincipal CustomUserDetails principal) {
        requireOwner(startupId, principal);
        return MvpFeatureResponse.from(mvpService.addFeature(startupId, mvpId, req));
    }

    @PutMapping("/{mvpId}/features/{featureId}")
    public MvpFeatureResponse updateFeature(@PathVariable Long startupId, @PathVariable Long mvpId,
                                            @PathVariable Long featureId,
                                            @Valid @RequestBody MvpFeatureRequest req,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        requireOwner(startupId, principal);
        return MvpFeatureResponse.from(mvpService.updateFeature(startupId, mvpId, featureId, req));
    }

    @PatchMapping("/{mvpId}/features/{featureId}/complete")
    public MvpFeatureResponse complete(@PathVariable Long startupId, @PathVariable Long mvpId,
                                       @PathVariable Long featureId,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        requireOwner(startupId, principal);
        return MvpFeatureResponse.from(mvpService.setFeatureCompleted(startupId, mvpId, featureId, true));
    }

    @PatchMapping("/{mvpId}/features/{featureId}/reopen")
    public MvpFeatureResponse reopen(@PathVariable Long startupId, @PathVariable Long mvpId,
                                     @PathVariable Long featureId,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        requireOwner(startupId, principal);
        return MvpFeatureResponse.from(mvpService.setFeatureCompleted(startupId, mvpId, featureId, false));
    }

    @DeleteMapping("/{mvpId}/features/{featureId}")
    public void deleteFeature(@PathVariable Long startupId, @PathVariable Long mvpId,
                              @PathVariable Long featureId,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        requireOwner(startupId, principal);
        mvpService.deleteFeature(startupId, mvpId, featureId);
    }

    private void requireOwner(Long startupId, CustomUserDetails principal) {
        accessGuard.requireOwner(principal.getUser(), startupService.findById(startupId));
    }
}
