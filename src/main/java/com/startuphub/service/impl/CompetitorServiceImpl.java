package com.startuphub.service.impl;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Competitor;
import com.startuphub.model.Startup;
import com.startuphub.repository.CompetitorRepository;
import com.startuphub.service.CompetitorService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CompetitorServiceImpl implements CompetitorService {
    private final CompetitorRepository competitorRepository;

    public CompetitorServiceImpl(CompetitorRepository competitorRepository) {
        this.competitorRepository = competitorRepository;
    }

    @Override
    public Competitor add(String name, double marketShare, String strengths, String weaknesses,
                          String opportunities, String threats, Startup startup) {
        Competitor c = new Competitor(name, marketShare, strengths, weaknesses, startup);
        c.setOpportunities(opportunities);
        c.setThreats(threats);
        return competitorRepository.save(c);
    }

    @Override
    public Competitor findById(Long competitorId) {
        return competitorRepository.findById(competitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Competitor not found: " + competitorId));
    }

    @Override
    public Competitor update(Long competitorId, String name, double marketShare, String strengths,
                             String weaknesses, String opportunities, String threats) {
        Competitor c = findById(competitorId);
        c.setName(name.trim());
        c.setMarketShare(marketShare);
        c.setStrengths(strengths);
        c.setWeaknesses(weaknesses);
        c.setOpportunities(opportunities);
        c.setThreats(threats);
        return competitorRepository.save(c);
    }

    @Override
    public void delete(Long competitorId) {
        competitorRepository.delete(findById(competitorId));
    }

    @Override
    public List<Competitor> findByStartup(Long startupId) { return competitorRepository.findByStartupId(startupId); }
}
