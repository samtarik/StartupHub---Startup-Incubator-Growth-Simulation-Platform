package com.startuphub.dto;

import com.startuphub.model.Campaign;

public record CampaignDto(Long id, String name, double budget, long reach, long conversions, double conversionRate) {
    public static CampaignDto from(Campaign c) {
        return new CampaignDto(c.getId(), c.getName(), c.getBudget(), c.getReach(), c.getConversions(), c.conversionRate());
    }
}
