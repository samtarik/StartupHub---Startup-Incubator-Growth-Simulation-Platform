package com.startuphub.dto;

import com.startuphub.model.MilestonePriority;
import com.startuphub.model.MilestoneStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** Full edit of a milestone: its content plus (optionally) its status and progress. */
public record MilestoneEditRequest(@NotBlank(message = "Milestone title is required") String title,
                                   String description, LocalDate deadline, MilestonePriority priority,
                                   MilestoneStatus status,
                                   @Min(value = 0, message = "Progress must be between 0 and 100")
                                   @Max(value = 100, message = "Progress must be between 0 and 100")
                                   Integer progress) {}
