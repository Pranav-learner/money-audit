package com.Pranav.finance_tracker.financialintelligence.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formats monetary and percentage values for human-readable insight messages.
 *
 * <p>Currency is rendered with Indian digit grouping (e.g. {@code ₹1,23,450}) — a group of
 * three least-significant digits followed by groups of two. This is implemented directly
 * because {@code java.text.DecimalFormat} supports only a single grouping size, and the result
 * is therefore deterministic across JVMs / locale providers. Keeping this logic here means
 * rules never hardcode formatting and every message reads consistently.</p>
 */
public final class MoneyFormatter {

    private MoneyFormatter() {
        // utility class
    }

    /** Formats an amount as whole rupees, e.g. {@code ₹6,450}. Null is treated as zero. */
    public static String rupees(BigDecimal amount) {
        BigDecimal value = (amount == null ? BigDecimal.ZERO : amount).setScale(0, RoundingMode.HALF_UP);
        boolean negative = value.signum() < 0;
        String digits = value.abs().toBigInteger().toString();
        String prefix = negative ? "-₹" : "₹";
        return prefix + groupIndian(digits);
    }

    /** Formats a whole-number percentage, e.g. {@code 28%}. */
    public static String percent(int value) {
        return value + "%";
    }

    /**
     * Applies Indian digit grouping to a non-negative integer string:
     * the last three digits form one group, then remaining digits are grouped in pairs.
     */
    private static String groupIndian(String digits) {
        int length = digits.length();
        if (length <= 3) {
            return digits;
        }
        String lastThree = digits.substring(length - 3);
        String rest = digits.substring(0, length - 3);

        StringBuilder grouped = new StringBuilder();
        int index = rest.length();
        while (index > 2) {
            grouped.insert(0, "," + rest.substring(index - 2, index));
            index -= 2;
        }
        grouped.insert(0, rest.substring(0, index));
        return grouped + "," + lastThree;
    }
}
