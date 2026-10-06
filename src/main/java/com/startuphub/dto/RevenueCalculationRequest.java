package com.startuphub.dto;

import com.startuphub.model.RevenueModel;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * One request shape for every revenue model. Only the fields that belong to the chosen
 * model are read (and validated) by its RevenueStrategy; the rest are ignored.
 *
 *  SUBSCRIPTION  : customers, monthlyPrice
 *  TRANSACTION   : transactions, averageTransactionValue, commissionPercent
 *  ADVERTISEMENT : impressions, cpm
 *
 * "source" is only used when the result is saved as a Revenue record.
 */
public record RevenueCalculationRequest(
        @NotNull(message = "Choose a revenue model") RevenueModel model,
        Long customers,
        BigDecimal monthlyPrice,
        Long transactions,
        BigDecimal averageTransactionValue,
        BigDecimal commissionPercent,
        Long impressions,
        BigDecimal cpm,
        String source) {}
