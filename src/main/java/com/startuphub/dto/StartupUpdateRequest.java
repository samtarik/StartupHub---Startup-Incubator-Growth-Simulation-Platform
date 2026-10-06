package com.startuphub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Edit a startup. Stage is deliberately not editable here - it only moves via advance-stage. */
public record StartupUpdateRequest(@NotBlank(message = "Startup name is required") String name,
                                   String description,
                                   @PositiveOrZero(message = "Valuation cannot be negative") Double valuation) {}
