package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small shared money helpers for recommendation rules, keeping rounding consistent across rules
 * (recommended figures read better rounded to a "clean" step such as ₹100).
 */
public final class Amounts {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Amounts() {
    }

    /** Rounds a value to the nearest ₹100 (HALF_UP). Null is treated as zero. */
    public static BigDecimal roundToHundred(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        return v.divide(HUNDRED, 0, RoundingMode.HALF_UP).multiply(HUNDRED);
    }
}
