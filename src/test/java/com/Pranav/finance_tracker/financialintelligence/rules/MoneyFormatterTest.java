package com.Pranav.finance_tracker.financialintelligence.rules;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyFormatterTest {

    @Test
    void formatsRupeesWithIndianGrouping() {
        assertThat(MoneyFormatter.rupees(new BigDecimal("6450"))).isEqualTo("₹6,450");
        assertThat(MoneyFormatter.rupees(new BigDecimal("123450"))).isEqualTo("₹1,23,450");
    }

    @Test
    void roundsToWholeRupees() {
        assertThat(MoneyFormatter.rupees(new BigDecimal("99.60"))).isEqualTo("₹100");
    }

    @Test
    void treatsNullAsZero() {
        assertThat(MoneyFormatter.rupees(null)).isEqualTo("₹0");
    }

    @Test
    void formatsPercent() {
        assertThat(MoneyFormatter.percent(28)).isEqualTo("28%");
    }
}
