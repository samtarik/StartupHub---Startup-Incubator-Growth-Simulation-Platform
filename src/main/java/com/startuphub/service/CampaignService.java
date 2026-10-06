package com.startuphub.service;

import com.startuphub.model.Campaign;
import com.startuphub.model.Startup;
import java.util.List;

public interface CampaignService {
    Campaign launch(String name, double budget, Startup startup);
    List<Campaign> findByStartup(Long startupId);
    Campaign findById(Long campaignId);
    Campaign updateMetrics(Long campaignId, long reach, long conversions);
    Campaign update(Long campaignId, String name, double budget, Long reach, Long conversions);
    void delete(Long campaignId);
}
