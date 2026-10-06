package com.startuphub.pattern.strategy;

import com.startuphub.dto.RevenueCalculationRequest;
import com.startuphub.dto.RevenueCalculationResponse;
import com.startuphub.model.RevenueModel;

/**
 * Strategy pattern: one interface, one implementation per revenue model.
 *
 * Two operations live here:
 *  - projectMonthlyRevenue : the ORIGINAL what-if projection used by the Overview
 *    "Project revenue" card (kept unchanged so existing behaviour is preserved).
 *  - calculate             : the AUTOMATIC REVENUE CALCULATOR. It validates the inputs of
 *    its own model and returns the authoritative amount using BigDecimal arithmetic.
 */
public interface RevenueStrategy {

    double projectMonthlyRevenue(double baseAmount, long customers);

    /** Which business model this strategy implements (used by RevenueStrategyResolver). */
    RevenueModel model();

    /** @throws IllegalArgumentException with a user-readable message for invalid input. */
    RevenueCalculationResponse calculate(RevenueCalculationRequest request);
}
