package com.startuphub.dto;

import com.startuphub.model.MilestonePriority;
import jakarta.validation.constraints.NotBlank;

public record MvpFeatureRequest(@NotBlank(message = "Feature title is required") String title,
                                String description,
                                MilestonePriority priority) {}
