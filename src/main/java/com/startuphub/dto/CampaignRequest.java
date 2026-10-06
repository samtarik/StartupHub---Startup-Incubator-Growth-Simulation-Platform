package com.startuphub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CampaignRequest(@NotBlank(message = "Campaign name is required") String name,
                              @PositiveOrZero(message = "Budget cannot be negative") double budget) {}
