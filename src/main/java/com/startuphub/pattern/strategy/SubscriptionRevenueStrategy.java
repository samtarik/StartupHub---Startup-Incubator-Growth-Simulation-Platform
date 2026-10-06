package com.startuphub.pattern.strategy;

import com.startuphub.dto.RevenueCalculationRequest;
import com.startuphub.dto.RevenueCalculationResponse;
import com.startuphub.model.RevenueModel;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("subscriptionRevenueStrategy")
public class SubscriptionRevenueStrategy implements RevenueStrategy {

    @Override
    public double projectMonthlyRevenue(double baseAmount, long customers) {
        return baseAmount * customers;
    }

    @Override
    public RevenueModel model() { return RevenueModel.SUBSCRIPTION; }

    /** Monthly revenue = paying customers x monthly subscription price. */
    @Override
    public RevenueCalculationResponse calculate(RevenueCalculationRequest req) {
        long customers = RevenueMath.requirePositiveCount(req.customers(), "Paying customers");
        BigDecimal price = RevenueMath.requirePositiveAmount(req.monthlyPrice(), "Subscription price");

        BigDecimal amount = RevenueMath.money(BigDecimal.valueOf(customers).multiply(price));
        String formula = customers + " customers \u00D7 " + RevenueMath.fmt(price) + "/month = " + RevenueMath.fmt(amount);
        return new RevenueCalculationResponse(model(), model().getLabel(), amount, formula);
    }
}
