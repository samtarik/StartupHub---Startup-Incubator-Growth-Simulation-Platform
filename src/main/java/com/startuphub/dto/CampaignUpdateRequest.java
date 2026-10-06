package com.startuphub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Edit a campaign. reach/conversions are optional: omit both to leave the metrics untouched. */
public record CampaignUpdateRequest(@NotBlank(message = "Campaign name is required") String name,
                                    @PositiveOrZero(message = "Budget cannot be negative") double budget,
                                    @PositiveOrZero(message = "Reach cannot be negative") Long reach,
                                    @PositiveOrZero(message = "Conversions cannot be negative") Long conversions) {}
