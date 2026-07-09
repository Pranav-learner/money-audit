package com.Pranav.finance_tracker.financialintelligence.risk.service;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.dto.RecurringCharge;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicRecurringChargeDetectorTest {

    private final HeuristicRecurringChargeDetector detector =
            new HeuristicRecurringChargeDetector(new RiskThresholdProperties());

    private InsightContext context(List<Expense> window) {
        LocalDate today = LocalDate.of(2026, 7, 10);
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(window)
                .budgetUsages(List.of())
                .build();
    }

    @Test
    void detectsAChargeRecurringAcrossThreeMonths() {
        var window = new ArrayList<Expense>();
        for (int month = 4; month <= 6; month++) {
            window.add(TestFixtures.expense("499", LocalDate.of(2026, month, 7), "Entertainment", "Gym"));
        }

        List<RecurringCharge> charges = detector.detect(context(window));

        assertThat(charges).hasSize(1);
        RecurringCharge charge = charges.get(0);
        assertThat(charge.getLabel()).isEqualTo("Gym");
        assertThat(charge.getMonthsObserved()).isEqualTo(3);
        assertThat(charge.getTypicalDayOfMonth()).isEqualTo(7);
        assertThat(charge.getTypicalAmount()).isEqualByComparingTo("499");
    }

    @Test
    void ignoresOneOffAndInfrequentCharges() {
        var window = List.of(
                TestFixtures.expense("499", LocalDate.of(2026, 6, 7), "Entertainment", "Gym"),
                TestFixtures.expense("2000", LocalDate.of(2026, 6, 9), "Shopping", "Shoes"));

        assertThat(detector.detect(context(window))).isEmpty();
    }
}
