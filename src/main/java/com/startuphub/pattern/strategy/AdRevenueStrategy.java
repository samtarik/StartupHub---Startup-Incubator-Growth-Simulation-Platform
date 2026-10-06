package com.startuphub.pattern.strategy;

import com.startuphub.dto.RevenueCalculationRequest;
import com.startuphub.dto.RevenueCalculationResponse;
import com.startuphub.model.RevenueModel;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("adRevenueStrategy")
public class AdRevenueStrategy implements RevenueStrategy {

    @Override
    public double projectMonthlyRevenue(double baseAmount, long customers) {
        return baseAmount * Math.sqrt(customers);
    }

    @Override
    public RevenueModel model() { return RevenueModel.ADVERTISEMENT; }

    /** Revenue = (impressions / 1000) x CPM, where CPM is the price per 1,000 impressions. */
    @Override
    public RevenueCalculationResponse calculate(RevenueCalculationRequest req) {
        long impressions = RevenueMath.requirePositiveCount(req.impressions(), "Number of impressions");
        BigDecimal cpm = RevenueMath.requirePositiveAmount(req.cpm(), "CPM");

        // Multiply first, divide once at the end, so no precision is lost on the way.
        BigDecimal amount = RevenueMath.money(
                BigDecimal.valueOf(impressions).multiply(cpm).divide(RevenueMath.THOUSAND, 2, RoundingMode.HALF_UP));
        String formula = "(" + String.format("%,d", impressions) + " impressions / 1,000) \u00D7 "
                + RevenueMath.fmt(cpm) + " CPM = " + RevenueMath.fmt(amount);
        return new RevenueCalculationResponse(model(), model().getLabel(), amount, formula);
    }
}
