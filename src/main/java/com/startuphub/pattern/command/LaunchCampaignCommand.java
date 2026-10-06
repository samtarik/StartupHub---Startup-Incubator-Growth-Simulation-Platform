package com.startuphub.pattern.command;

import com.startuphub.model.Campaign;
import com.startuphub.model.Startup;
import com.startuphub.repository.CampaignRepository;

public class LaunchCampaignCommand implements Command {
    private final CampaignRepository campaignRepository;
    private final String name;
    private final double budget;
    private final Startup startup;
    private Campaign result;

    public LaunchCampaignCommand(CampaignRepository campaignRepository, String name, double budget, Startup startup) {
        this.campaignRepository = campaignRepository;
        this.name = name;
        this.budget = budget;
        this.startup = startup;
    }

    @Override
    public void execute() {
        result = campaignRepository.save(new Campaign(name, budget, startup));
    }

    public Campaign getResult() { return result; }

    @Override
    public String describe() {
        return "Launch campaign '" + name + "' with budget " + budget + " for " + startup.getName();
    }
}
