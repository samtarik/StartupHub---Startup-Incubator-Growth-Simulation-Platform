package com.startuphub.pattern.command;

import com.startuphub.model.Campaign;
import com.startuphub.repository.CampaignRepository;

/** Command pattern: editing a campaign's details/metrics lands in the same audit history as launching one. */
public class UpdateCampaignCommand implements Command {
    private final CampaignRepository campaignRepository;
    private final Campaign campaign;
    private final String name;
    private final double budget;
    private final Long reach;
    private final Long conversions;
    private Campaign result;

    public UpdateCampaignCommand(CampaignRepository campaignRepository, Campaign campaign, String name,
                                 double budget, Long reach, Long conversions) {
        this.campaignRepository = campaignRepository;
        this.campaign = campaign;
        this.name = name;
        this.budget = budget;
        this.reach = reach;
        this.conversions = conversions;
    }

    @Override
    public void execute() {
        campaign.setName(name);
        campaign.setBudget(budget);
        if (reach != null || conversions != null) {
            long newReach = reach != null ? reach : campaign.getReach();
            long newConversions = conversions != null ? conversions : campaign.getConversions();
            campaign.setReach(newReach);
            campaign.setConversions(Math.min(newConversions, newReach));
        }
        result = campaignRepository.save(campaign);
    }

    public Campaign getResult() { return result; }

    @Override
    public String describe() {
        return "Update campaign #" + campaign.getId() + " '" + name + "' (budget " + budget + ")";
    }
}
