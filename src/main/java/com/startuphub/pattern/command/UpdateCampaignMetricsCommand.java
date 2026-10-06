package com.startuphub.pattern.command;

import com.startuphub.model.Campaign;
import com.startuphub.repository.CampaignRepository;

/**
 * Command pattern: recording a campaign's real reach/conversion numbers is an
 * action, so it runs through CommandInvoker like every other campaign action
 * and lands in the same audit history.
 */
public class UpdateCampaignMetricsCommand implements Command {
    private final CampaignRepository campaignRepository;
    private final Campaign campaign;
    private final long reach;
    private final long conversions;
    private Campaign result;

    public UpdateCampaignMetricsCommand(CampaignRepository campaignRepository, Campaign campaign,
                                        long reach, long conversions) {
        this.campaignRepository = campaignRepository;
        this.campaign = campaign;
        this.reach = reach;
        this.conversions = conversions;
    }

    @Override
    public void execute() {
        campaign.setReach(reach);
        campaign.setConversions(Math.min(conversions, reach));
        result = campaignRepository.save(campaign);
    }

    public Campaign getResult() { return result; }

    @Override
    public String describe() {
        return "Record metrics for campaign '" + campaign.getName() + "': reach=" + reach
                + ", conversions=" + conversions;
    }
}
