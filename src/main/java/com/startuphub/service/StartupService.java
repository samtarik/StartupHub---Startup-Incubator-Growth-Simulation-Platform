package com.startuphub.service;

import com.startuphub.model.Startup;
import com.startuphub.model.User;
import java.util.List;

public interface StartupService {
    Startup create(String name, String description, User founder);
    List<Startup> findAll();
    List<Startup> findByFounder(Long founderId);
    Startup findById(Long id);
    Startup advanceStage(Long id);
    void delete(Long id);
    Startup update(Long id, String name, String description, Double valuation);
}
