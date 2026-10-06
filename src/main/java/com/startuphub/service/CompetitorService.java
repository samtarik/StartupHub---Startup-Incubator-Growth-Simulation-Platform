package com.startuphub.service;

import com.startuphub.model.Competitor;
import com.startuphub.model.Startup;
import java.util.List;

public interface CompetitorService {
    Competitor add(String name, double marketShare, String strengths, String weaknesses,
                   String opportunities, String threats, Startup startup);
    Competitor findById(Long competitorId);
    Competitor update(Long competitorId, String name, double marketShare, String strengths, String weaknesses,
                      String opportunities, String threats);
    void delete(Long competitorId);
    List<Competitor> findByStartup(Long startupId);
}
