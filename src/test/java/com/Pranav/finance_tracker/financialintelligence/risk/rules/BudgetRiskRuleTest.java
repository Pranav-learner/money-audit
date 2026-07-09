package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.impl.BudgetRiskRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetRiskRuleTest {

    private final BudgetRiskRule rule = new BudgetRiskRule(new RiskThresholdProperties());

    @Test
    void highSeverityWhenAnyBudgetIsOverTheLimit() {
        var context = TestFixtures.riskContext()
                .budgetUsages(List.of(
                        TestFixtures.budgetUsage("Food", "5000", "5500", "-500", 110, "OVER_BUDGET"),
                        TestFixtures.budgetUsage("Transport", "2000", "1700", "300", 85, "NEAR_LIMIT")))
                .build();

        List<InsightDraft> drafts = rule.evaluate(context);

        assertThat(drafts).hasSize(1);
        InsightDraft draft = drafts.get(0);
        assertThat(draft.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(draft.getRiskType()).isEqualTo(FinancialRiskType.BUDGET_RISK);
        assertThat(draft.getCategory()).isEqualTo("Food"); // most-strained category surfaced
        assertThat(draft.getDescription()).contains("2 budgets are at risk").contains("110%");
    }

    @Test
    void mediumSeverityWhenOnlyNearingTheLimit() {
        var context = TestFixtures.riskContext()
                .budgetUsages(List.of(
                        TestFixtures.budgetUsage("Food", "5000", "4200", "800", 84, "NEAR_LIMIT")))
                .build();

        List<InsightDraft> drafts = rule.evaluate(context);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(drafts.get(0).getDescription()).contains("84%");
    }

    @Test
    void noRiskWhenAllBudgetsHealthy() {
        var context = TestFixtures.riskContext()
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "2000", "3000", 40, "NORMAL")))
                .build();

        assertThat(rule.evaluate(context)).isEmpty();
    }

    @Test
    void noRiskWhenNoBudgets() {
        assertThat(rule.evaluate(TestFixtures.riskContext().build())).isEmpty();
    }
}
