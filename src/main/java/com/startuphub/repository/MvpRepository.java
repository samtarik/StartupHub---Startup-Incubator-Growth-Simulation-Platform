package com.startuphub.repository;

import com.startuphub.model.Mvp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MvpRepository extends JpaRepository<Mvp, Long> {
    Optional<Mvp> findByStartupId(Long startupId);
}
