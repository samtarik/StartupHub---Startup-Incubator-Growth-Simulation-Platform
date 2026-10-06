package com.startuphub.repository;

import com.startuphub.model.MvpFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MvpFeatureRepository extends JpaRepository<MvpFeature, Long> {
    List<MvpFeature> findByMvpId(Long mvpId);
}
