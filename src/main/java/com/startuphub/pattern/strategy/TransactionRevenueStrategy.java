package com.startuphub.pattern.strategy;

import com.startuphub.dto.RevenueCalculationRequest;
import com.startuphub.dto.RevenueCalculationResponse;
import com.startuphub.model.RevenueModel;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("transactionRevenueStrategy")
public class TransactionRevenueStrategy implements RevenueStrategy {

    @Override
    public double projectMonthlyRevenue(double baseAmount, long customers) {
        return baseAmount * customers * 4;
    }

    @Override
    public RevenueModel model() { return RevenueModel.TRANSACTION; }

    /** Revenue = transactions x average transaction value x (commission % / 100). */
    @Override
    public RevenueCalculationResponse calculate(RevenueCalculationRequest req) {
        long transactions = RevenueMath.requirePositiveCount(req.transactions(), "Number of transactions");
        BigDecimal average = RevenueMath.requirePositiveAmount(req.averageTransactionValue(), "Average transaction value");
        BigDecimal percent = RevenueMath.requirePercentage(req.commissionPercent(), "Commission percentage");

        BigDecimal rate = percent.divide(RevenueMath.HUNDRED);
        BigDecimal amount = RevenueMath.money(BigDecimal.valueOf(transactions).multiply(average).multiply(rate));
        String formula = transactions + " transactions \u00D7 " + RevenueMath.fmt(average) + " \u00D7 "
                + RevenueMath.plain(percent) + "% = " + RevenueMath.fmt(amount);
        return new RevenueCalculationResponse(model(), model().getLabel(), amount, formula);
    }
}
