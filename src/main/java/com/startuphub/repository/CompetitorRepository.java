package com.startuphub.repository;

import com.startuphub.model.Competitor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompetitorRepository extends JpaRepository<Competitor, Long> {
    List<Competitor> findByStartupId(Long startupId);
}
