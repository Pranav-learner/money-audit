package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.rules.impl.BudgetUsageRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetUsageRuleTest {

    private final BudgetUsageRule rule = new BudgetUsageRule();

    private InsightContext contextWithBudgets(List<com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse> usages) {
        LocalDate today = LocalDate.now();
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(List.of())
                .budgetUsages(usages)
                .build();
    }

    @Test
    void emitsHighSeverityWhenOverBudgetAndMediumWhenNearLimit() {
        var usages = List.of(
                TestFixtures.budgetUsage("Food", "5000", "5500", "-500", 110, "OVER_BUDGET"),
                TestFixtures.budgetUsage("Transport", "2000", "1700", "300", 85, "NEAR_LIMIT"));

        List<InsightDraft> drafts = rule.evaluate(contextWithBudgets(usages));

        assertThat(drafts).hasSize(2);
        assertThat(drafts).allMatch(d -> d.getInsightType() == InsightType.BUDGET_ALERT);

        InsightDraft over = drafts.stream().filter(d -> d.getCategory().equals("Food")).findFirst().orElseThrow();
        assertThat(over.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(over.getRuleKey()).isEqualTo("BUDGET_USAGE:Food");
        assertThat(over.getDescription()).contains("110%");

        InsightDraft near = drafts.stream().filter(d -> d.getCategory().equals("Transport")).findFirst().orElseThrow();
        assertThat(near.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(near.getRuleKey()).isEqualTo("BUDGET_USAGE:Transport");
    }

    @Test
    void noInsightWhenAllBudgetsHealthy() {
        var usages = List.of(TestFixtures.budgetUsage("Food", "5000", "2000", "3000", 40, "NORMAL"));
        assertThat(rule.evaluate(contextWithBudgets(usages))).isEmpty();
    }

    @Test
    void noInsightWhenNoBudgets() {
        assertThat(rule.evaluate(contextWithBudgets(List.of()))).isEmpty();
    }
}
