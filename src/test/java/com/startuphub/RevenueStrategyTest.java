package com.startuphub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.startuphub.dto.RevenueCalculationRequest;
import com.startuphub.model.RevenueModel;
import com.startuphub.pattern.strategy.AdRevenueStrategy;
import com.startuphub.pattern.strategy.RevenueStrategyResolver;
import com.startuphub.pattern.strategy.SubscriptionRevenueStrategy;
import com.startuphub.pattern.strategy.TransactionRevenueStrategy;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Plain unit tests (no Spring context): the strategies and the resolver are ordinary objects. */
class RevenueStrategyTest {

    private final RevenueStrategyResolver resolver = new RevenueStrategyResolver(List.of(
            new SubscriptionRevenueStrategy(), new TransactionRevenueStrategy(), new AdRevenueStrategy()));

    private BigDecimal calc(RevenueCalculationRequest req) {
        return resolver.resolve(req.model()).calculate(req).amount();
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void subscriptionIsCustomersTimesPrice() {
        assertAmount("2000", calc(new RevenueCalculationRequest(RevenueModel.SUBSCRIPTION,
                100L, new BigDecimal("20"), null, null, null, null, null, null)));
    }

    @Test
    void transactionIsCountTimesAverageTimesCommission() {
        assertAmount("1000", calc(new RevenueCalculationRequest(RevenueModel.TRANSACTION,
                null, null, 500L, new BigDecimal("40"), new BigDecimal("5"), null, null, null)));
    }

    @Test
    void advertisementIsImpressionsPerThousandTimesCpm() {
        assertAmount("500", calc(new RevenueCalculationRequest(RevenueModel.ADVERTISEMENT,
                null, null, null, null, null, 100000L, new BigDecimal("5"), null)));
    }

    @Test
    void invalidInputsAreRejectedWithAMessage() {
        assertThrows(IllegalArgumentException.class, () -> calc(new RevenueCalculationRequest(
                RevenueModel.SUBSCRIPTION, -1L, new BigDecimal("20"), null, null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> calc(new RevenueCalculationRequest(
                RevenueModel.SUBSCRIPTION, 10L, new BigDecimal("0"), null, null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> calc(new RevenueCalculationRequest(
                RevenueModel.TRANSACTION, null, null, 10L, new BigDecimal("40"), new BigDecimal("101"), null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> calc(new RevenueCalculationRequest(
                RevenueModel.ADVERTISEMENT, null, null, null, null, null, 0L, new BigDecimal("5"), null)));
    }

    @Test
    void originalProjectionStrategiesAreUnchanged() {
        assertEquals(2000.0, new SubscriptionRevenueStrategy().projectMonthlyRevenue(20, 100), 0.0001);
        assertEquals(8000.0, new TransactionRevenueStrategy().projectMonthlyRevenue(20, 100), 0.0001);
    }
}
