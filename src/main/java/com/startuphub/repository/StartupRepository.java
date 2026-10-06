package com.startuphub.repository;

import com.startuphub.model.Startup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StartupRepository extends JpaRepository<Startup, Long> {
    List<Startup> findByFounderId(Long founderId);
}
