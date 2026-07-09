package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.impl.SubscriptionRiskRule;
import com.Pranav.finance_tracker.financialintelligence.risk.service.HeuristicRecurringChargeDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionRiskRuleTest {

    private final RiskThresholdProperties props = new RiskThresholdProperties();
    private final SubscriptionRiskRule rule =
            new SubscriptionRiskRule(new HeuristicRecurringChargeDetector(props), props);

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 10);

    private InsightContext context(List<Expense> window) {
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(TODAY)
                .currentMonth(YearMonth.from(TODAY))
                .previousMonth(YearMonth.from(TODAY).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(window)
                .budgetUsages(List.of())
                .build();
    }

    @Test
    void flagsAnExpensiveRecurringChargeWithoutHardcodingMerchant() {
        var window = new ArrayList<Expense>();
        for (int month = 4; month <= 6; month++) {
            window.add(TestFixtures.expense("599", LocalDate.of(2026, month, 5), "Entertainment", "Streaming Plus"));
        }
        // A cheap one-off that must NOT be flagged.
        window.add(TestFixtures.expense("50", LocalDate.of(2026, 6, 12), "Food", "Coffee"));

        List<InsightDraft> drafts = rule.evaluate(context(window));

        assertThat(drafts).hasSize(1);
        InsightDraft draft = drafts.get(0);
        assertThat(draft.getRiskType()).isEqualTo(FinancialRiskType.SUBSCRIPTION_RISK);
        assertThat(draft.getRuleKey()).isEqualTo("SUBSCRIPTION_RISK:Streaming Plus");
        assertThat(draft.getDescription()).contains("Streaming Plus").contains("₹599");
    }

    @Test
    void ignoresCheapRecurringChargesBelowCostThreshold() {
        var window = new ArrayList<Expense>();
        for (int month = 4; month <= 6; month++) {
            window.add(TestFixtures.expense("120", LocalDate.of(2026, month, 8), "Entertainment", "Music Lite"));
        }
        assertThat(rule.evaluate(context(window))).isEmpty();
    }

    @Test
    void ignoresChargesThatDoNotRecurEnough() {
        var window = List.of(
                TestFixtures.expense("999", LocalDate.of(2026, 6, 5), "Entertainment", "Streaming Plus"),
                TestFixtures.expense("999", LocalDate.of(2026, 5, 5), "Entertainment", "Streaming Plus"));
        assertThat(rule.evaluate(context(window))).isEmpty(); // only 2 months < min 3
    }
}
