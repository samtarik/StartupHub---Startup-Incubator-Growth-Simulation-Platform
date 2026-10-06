package com.startuphub.service.impl;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Milestone;
import com.startuphub.model.MilestonePriority;
import com.startuphub.model.MilestoneStatus;
import com.startuphub.model.Roadmap;
import com.startuphub.model.Startup;
import com.startuphub.pattern.observer.MilestoneCompletedEvent;
import com.startuphub.repository.MilestoneRepository;
import com.startuphub.repository.RoadmapRepository;
import com.startuphub.service.RoadmapService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class RoadmapServiceImpl implements RoadmapService {
    private final RoadmapRepository roadmapRepository;
    private final MilestoneRepository milestoneRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RoadmapServiceImpl(RoadmapRepository roadmapRepository, MilestoneRepository milestoneRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.roadmapRepository = roadmapRepository;
        this.milestoneRepository = milestoneRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Roadmap createRoadmap(String title, Startup startup) {
        return roadmapRepository.save(new Roadmap(title, startup));
    }

    @Override
    public List<Roadmap> findByStartup(Long startupId) { return roadmapRepository.findByStartupId(startupId); }

    @Override
    public Roadmap findRoadmap(Long roadmapId) {
        return roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap not found: " + roadmapId));
    }

    @Override
    public Milestone findMilestone(Long milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found: " + milestoneId));
    }

    @Override
    public Milestone addMilestone(Long roadmapId, String title, String description,
                                  LocalDate deadline, MilestonePriority priority) {
        Roadmap roadmap = findRoadmap(roadmapId);
        return milestoneRepository.save(new Milestone(title, description, deadline, priority, roadmap));
    }

    @Override
    public Milestone completeMilestone(Long milestoneId) {
        return updateMilestone(milestoneId, MilestoneStatus.DONE, 100);
    }

    /**
     * The single place where a milestone changes state. When it lands on DONE we publish
     * MilestoneCompletedEvent - the Observer pattern then creates the notification and
     * pushes it over WebSocket, without this service knowing either exists.
     */
    @Override
    public Milestone updateMilestone(Long milestoneId, MilestoneStatus status, Integer progress) {
        Milestone m = findMilestone(milestoneId);
        boolean wasAlreadyDone = m.getStatus() == MilestoneStatus.DONE;

        if (status != null) m.setStatus(status);
        if (progress != null) m.setProgress(progress);

        if (m.getStatus() == MilestoneStatus.DONE) {
            m.setProgress(100);
        } else if (m.getStatus() == MilestoneStatus.PENDING && progress == null) {
            m.setProgress(0);
        }

        m = milestoneRepository.save(m);

        if (!wasAlreadyDone && m.getStatus() == MilestoneStatus.DONE) {
            eventPublisher.publishEvent(new MilestoneCompletedEvent(this, m));
        }
        return m;
    }

    @Override
    public Roadmap updateRoadmap(Long roadmapId, String title) {
        Roadmap r = findRoadmap(roadmapId);
        r.setTitle(title.trim());
        return roadmapRepository.save(r);
    }

    /** Milestones cascade with their roadmap (Roadmap.milestones is cascade = ALL, orphanRemoval). */
    @Override
    public void deleteRoadmap(Long roadmapId) {
        roadmapRepository.delete(findRoadmap(roadmapId));
    }

    /**
     * Edits the content of a milestone. Status/progress changes are handed to updateMilestone,
     * so completing a milestone through the edit form still publishes MilestoneCompletedEvent
     * (Observer pattern) exactly like the Mark done button does.
     */
    @Override
    public Milestone editMilestone(Long milestoneId, String title, String description, LocalDate deadline,
                                   MilestonePriority priority, MilestoneStatus status, Integer progress) {
        Milestone m = findMilestone(milestoneId);
        m.setTitle(title.trim());
        m.setDescription(description == null || description.isBlank() ? null : description.trim());
        m.setDeadline(deadline);
        if (priority != null) m.setPriority(priority);
        milestoneRepository.save(m);

        if (status != null || progress != null) {
            return updateMilestone(milestoneId, status, progress);
        }
        return m;
    }

    @Override
    public void deleteMilestone(Long milestoneId) {
        Milestone m = findMilestone(milestoneId);
        // Remove it from the parent's collection too, otherwise orphanRemoval would keep it managed.
        m.getRoadmap().getMilestones().remove(m);
        milestoneRepository.delete(m);
    }
}
