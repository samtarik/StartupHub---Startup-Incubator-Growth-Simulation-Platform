package com.startuphub.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record CompetitorRequest(@NotBlank(message = "Competitor name is required") String name,
                                @DecimalMin(value = "0", message = "Market share must be between 0 and 100")
                                @DecimalMax(value = "100", message = "Market share must be between 0 and 100")
                                double marketShare,
                                String strengths, String weaknesses,
                                String opportunities, String threats) {}
