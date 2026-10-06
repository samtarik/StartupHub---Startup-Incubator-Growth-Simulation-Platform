package com.startuphub.dto;

import com.startuphub.model.InvestmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InvestmentRequest(@NotNull(message = "Investment type is required") InvestmentType type,
                                @Positive(message = "Amount must be greater than zero") double amount) {}
