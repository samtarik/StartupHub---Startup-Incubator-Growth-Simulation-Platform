package com.startuphub.repository;

import com.startuphub.model.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {
    List<Roadmap> findByStartupId(Long startupId);
}
