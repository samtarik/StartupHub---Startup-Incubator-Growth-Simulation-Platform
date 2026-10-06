package com.startuphub.dto;

import com.startuphub.model.MilestoneStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Moves a milestone along the timeline without deleting and recreating it. */
public record MilestoneUpdateRequest(MilestoneStatus status,
                                     @Min(0) @Max(100) Integer progress) {}
