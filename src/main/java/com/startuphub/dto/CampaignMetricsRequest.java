package com.startuphub.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Records a campaign's real outcome so the performance chart has real data. */
public record CampaignMetricsRequest(@PositiveOrZero(message = "Reach cannot be negative") long reach,
                                     @PositiveOrZero(message = "Conversions cannot be negative") long conversions) {}
