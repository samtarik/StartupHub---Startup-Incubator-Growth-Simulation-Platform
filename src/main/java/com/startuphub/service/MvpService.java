package com.startuphub.service;

import com.startuphub.dto.MvpFeatureRequest;
import com.startuphub.dto.MvpRequest;
import com.startuphub.model.Mvp;
import com.startuphub.model.MvpFeature;
import com.startuphub.model.Startup;
import java.util.Optional;

public interface MvpService {
    Optional<Mvp> findByStartup(Long startupId);
    /** Loads the MVP and checks it belongs to the given startup (404 otherwise). */
    Mvp findForStartup(Long startupId, Long mvpId);
    Mvp create(Startup startup, MvpRequest request);
    Mvp update(Long startupId, Long mvpId, MvpRequest request);
    void delete(Long startupId, Long mvpId);

    MvpFeature addFeature(Long startupId, Long mvpId, MvpFeatureRequest request);
    MvpFeature updateFeature(Long startupId, Long mvpId, Long featureId, MvpFeatureRequest request);
    /** Marks a feature complete or reopens it. Progress is recalculated from the features. */
    MvpFeature setFeatureCompleted(Long startupId, Long mvpId, Long featureId, boolean completed);
    void deleteFeature(Long startupId, Long mvpId, Long featureId);
}
