package com.startuphub.dto;

import com.startuphub.model.RevenueModel;
import java.math.BigDecimal;

/** Authoritative result of a calculation, produced by the server-side strategy. */
public record RevenueCalculationResponse(RevenueModel model, String modelLabel,
                                         BigDecimal amount, String formula) {}
