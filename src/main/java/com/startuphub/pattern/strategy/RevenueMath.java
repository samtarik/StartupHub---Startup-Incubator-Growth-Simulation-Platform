package com.startuphub.pattern.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Shared, stateless helpers for the revenue strategies: validation and money formatting. */
final class RevenueMath {

    private RevenueMath() {}

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    /** Whole-number input that must be strictly positive. */
    static long requirePositiveCount(Long value, String label) {
        if (value == null) throw new IllegalArgumentException(label + " is required.");
        if (value <= 0) throw new IllegalArgumentException(label + " must be greater than 0.");
        return value;
    }

    /** Money-style input that must be strictly positive. */
    static BigDecimal requirePositiveAmount(BigDecimal value, String label) {
        if (value == null) throw new IllegalArgumentException(label + " is required.");
        if (value.signum() <= 0) throw new IllegalArgumentException(label + " must be greater than 0.");
        return value;
    }

    static BigDecimal requirePercentage(BigDecimal value, String label) {
        if (value == null) throw new IllegalArgumentException(label + " is required.");
        if (value.signum() <= 0 || value.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException(label + " must be greater than 0 and at most 100.");
        }
        return value;
    }

    static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    static String fmt(BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
        return nf.format(money(value));
    }

    static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
