package com.startuphub.service.impl;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Campaign;
import com.startuphub.model.Startup;
import com.startuphub.pattern.command.CommandInvoker;
import com.startuphub.pattern.command.LaunchCampaignCommand;
import com.startuphub.pattern.command.UpdateCampaignCommand;
import com.startuphub.pattern.command.UpdateCampaignMetricsCommand;
import com.startuphub.repository.CampaignRepository;
import com.startuphub.service.CampaignService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
    private final CommandInvoker commandInvoker;

    public CampaignServiceImpl(CampaignRepository campaignRepository, CommandInvoker commandInvoker) {
        this.campaignRepository = campaignRepository;
        this.commandInvoker = commandInvoker;
    }

    // Command pattern: build a command object, hand it to the invoker, which executes
    // it and keeps an audit trail (CommandInvoker.getHistory()).
    @Override
    public Campaign launch(String name, double budget, Startup startup) {
        LaunchCampaignCommand cmd = new LaunchCampaignCommand(campaignRepository, name, budget, startup);
        commandInvoker.run(cmd);
        return cmd.getResult();
    }

    @Override
    public List<Campaign> findByStartup(Long startupId) { return campaignRepository.findByStartupId(startupId); }

    @Override
    public Campaign findById(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
    }

    @Override
    public Campaign updateMetrics(Long campaignId, long reach, long conversions) {
        Campaign campaign = findById(campaignId);
        UpdateCampaignMetricsCommand cmd =
                new UpdateCampaignMetricsCommand(campaignRepository, campaign, reach, conversions);
        commandInvoker.run(cmd);
        return cmd.getResult();
    }

    @Override
    public Campaign update(Long campaignId, String name, double budget, Long reach, Long conversions) {
        Campaign campaign = findById(campaignId);
        long effectiveReach = reach != null ? reach : campaign.getReach();
        long effectiveConversions = conversions != null ? conversions : campaign.getConversions();
        if (effectiveConversions > effectiveReach) {
            throw new IllegalArgumentException("Conversions cannot be higher than reach.");
        }
        UpdateCampaignCommand cmd = new UpdateCampaignCommand(campaignRepository, campaign,
                name.trim(), budget, reach, conversions);
        commandInvoker.run(cmd);
        return cmd.getResult();
    }

    @Override
    public void delete(Long campaignId) {
        campaignRepository.delete(findById(campaignId));
    }
}
