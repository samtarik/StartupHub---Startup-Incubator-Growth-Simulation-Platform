package com.startuphub.dto;

import com.startuphub.model.Competitor;

public record CompetitorDto(Long id, String name, double marketShare, String strengths, String weaknesses,
                            String opportunities, String threats) {
    public static CompetitorDto from(Competitor c) {
        return new CompetitorDto(c.getId(), c.getName(), c.getMarketShare(), c.getStrengths(), c.getWeaknesses(),
                c.getOpportunities(), c.getThreats());
    }
}
