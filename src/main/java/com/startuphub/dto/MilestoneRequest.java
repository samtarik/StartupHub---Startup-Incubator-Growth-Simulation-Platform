package com.startuphub.dto;

import com.startuphub.model.MilestonePriority;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record MilestoneRequest(@NotBlank(message = "Milestone title is required") String title,
                               String description, LocalDate deadline, MilestonePriority priority) {}
