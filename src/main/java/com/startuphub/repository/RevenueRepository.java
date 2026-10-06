package com.startuphub.repository;

import com.startuphub.model.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RevenueRepository extends JpaRepository<Revenue, Long> {
    List<Revenue> findByStartupId(Long startupId);
}
