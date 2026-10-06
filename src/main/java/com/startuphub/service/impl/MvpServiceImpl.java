package com.startuphub.service.impl;

import com.startuphub.dto.MvpFeatureRequest;
import com.startuphub.dto.MvpRequest;
import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Mvp;
import com.startuphub.model.MvpFeature;
import com.startuphub.model.MvpStatus;
import com.startuphub.model.Startup;
import com.startuphub.pattern.observer.MvpFeatureCompletedEvent;
import com.startuphub.pattern.observer.MvpLaunchedEvent;
import com.startuphub.repository.MvpRepository;
import com.startuphub.service.MvpService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MVP management. Features are always reached through their MVP (the aggregate root), which
 * keeps the ownership chain Startup -> Mvp -> MvpFeature intact: a feature id from another
 * startup's MVP is simply "not found" here.
 *
 * Observer pattern: completing a feature or launching the MVP publishes an event; the
 * listener creates the notification (Factory pattern) and pushes it over WebSocket.
 */
@Service
public class MvpServiceImpl implements MvpService {

    private final MvpRepository mvpRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MvpServiceImpl(MvpRepository mvpRepository, ApplicationEventPublisher eventPublisher) {
        this.mvpRepository = mvpRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<Mvp> findByStartup(Long startupId) { return mvpRepository.findByStartupId(startupId); }

    @Override
    public Mvp findForStartup(Long startupId, Long mvpId) {
        Mvp mvp = mvpRepository.findById(mvpId)
                .orElseThrow(() -> new ResourceNotFoundException("MVP not found: " + mvpId));
        if (!mvp.getStartup().getId().equals(startupId)) {
            throw new ResourceNotFoundException("MVP not found: " + mvpId);
        }
        return mvp;
    }

    @Override
    @Transactional
    public Mvp create(Startup startup, MvpRequest req) {
        if (mvpRepository.findByStartupId(startup.getId()).isPresent()) {
            throw new IllegalArgumentException("This startup already has an MVP. Edit it instead of creating another.");
        }
        Mvp mvp = new Mvp(startup, req.name().trim(), clean(req.description()), req.version().trim(),
                req.status(), clean(req.targetUsers()), req.launchDate());
        applyLaunchRules(mvp);
        mvp = mvpRepository.save(mvp);
        if (mvp.getStatus() == MvpStatus.LAUNCHED) eventPublisher.publishEvent(new MvpLaunchedEvent(this, mvp));
        return mvp;
    }

    @Override
    @Transactional
    public Mvp update(Long startupId, Long mvpId, MvpRequest req) {
        Mvp mvp = findForStartup(startupId, mvpId);
        MvpStatus previous = mvp.getStatus();

        mvp.setName(req.name().trim());
        mvp.setDescription(clean(req.description()));
        mvp.setVersion(req.version().trim());
        mvp.setTargetUsers(clean(req.targetUsers()));
        mvp.setLaunchDate(req.launchDate());
        if (req.status() != null) mvp.setStatus(req.status());
        applyLaunchRules(mvp);
        mvp.setUpdatedAt(LocalDateTime.now());

        mvp = mvpRepository.save(mvp);
        if (previous != MvpStatus.LAUNCHED && mvp.getStatus() == MvpStatus.LAUNCHED) {
            eventPublisher.publishEvent(new MvpLaunchedEvent(this, mvp));
        }
        return mvp;
    }

    @Override
    @Transactional
    public void delete(Long startupId, Long mvpId) {
        mvpRepository.delete(findForStartup(startupId, mvpId)); // features cascade
    }

    // ------------------------------------------------------------------ features

    @Override
    @Transactional
    public MvpFeature addFeature(Long startupId, Long mvpId, MvpFeatureRequest req) {
        Mvp mvp = findForStartup(startupId, mvpId);
        MvpFeature feature = new MvpFeature(mvp, req.title().trim(), clean(req.description()), req.priority());
        mvp.getFeatures().add(feature);
        touch(mvp);
        // The Mvp is already managed, so no save() is needed (a merge would copy the new feature).
        // flush() persists it via the cascade and assigns its id before we return it.
        mvpRepository.flush();
        return feature;
    }

    @Override
    @Transactional
    public MvpFeature updateFeature(Long startupId, Long mvpId, Long featureId, MvpFeatureRequest req) {
        Mvp mvp = findForStartup(startupId, mvpId);
        MvpFeature feature = featureOf(mvp, featureId);
        feature.setTitle(req.title().trim());
        feature.setDescription(clean(req.description()));
        if (req.priority() != null) feature.setPriority(req.priority());
        feature.setUpdatedAt(LocalDateTime.now());
        touch(mvp); // managed entities are written back by dirty checking at commit
        return feature;
    }

    @Override
    @Transactional
    public MvpFeature setFeatureCompleted(Long startupId, Long mvpId, Long featureId, boolean completed) {
        Mvp mvp = findForStartup(startupId, mvpId);
        MvpFeature feature = featureOf(mvp, featureId);
        boolean changed = feature.isCompleted() != completed;

        feature.markCompleted(completed);
        feature.setUpdatedAt(LocalDateTime.now());
        touch(mvp);

        if (changed && completed) eventPublisher.publishEvent(new MvpFeatureCompletedEvent(this, feature));
        return feature;
    }

    @Override
    @Transactional
    public void deleteFeature(Long startupId, Long mvpId, Long featureId) {
        Mvp mvp = findForStartup(startupId, mvpId);
        MvpFeature feature = featureOf(mvp, featureId);
        mvp.getFeatures().remove(feature); // orphanRemoval deletes the row
        touch(mvp);
    }

    // ------------------------------------------------------------------ helpers

    private MvpFeature featureOf(Mvp mvp, Long featureId) {
        return mvp.getFeatures().stream()
                .filter(f -> f.getId().equals(featureId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("MVP feature not found: " + featureId));
    }

    /** A launched MVP always has a launch date: default it to today if the user left it empty. */
    private void applyLaunchRules(Mvp mvp) {
        if (mvp.getStatus() == MvpStatus.LAUNCHED && mvp.getLaunchDate() == null) {
            mvp.setLaunchDate(LocalDate.now());
        }
    }

    private void touch(Mvp mvp) { mvp.setUpdatedAt(LocalDateTime.now()); }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
