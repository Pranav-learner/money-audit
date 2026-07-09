package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.impl.DebtRiskRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DebtRiskRuleTest {

    private final DebtRiskRule rule = new DebtRiskRule(new RiskThresholdProperties());

    @Test
    void raisesRiskWhenOwedAmountExceedsThreshold() {
        var context = TestFixtures.riskContext()
                .totalOwed(new BigDecimal("18500"))
                .owedSettlementCount(5)
                .build();

        List<InsightDraft> drafts = rule.evaluate(context);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRiskType()).isEqualTo(FinancialRiskType.DEBT_RISK);
        assertThat(drafts.get(0).getSeverity()).isEqualTo(Severity.MEDIUM); // over threshold, under 2× cutoff
        assertThat(drafts.get(0).getDescription()).contains("₹18,500").contains("5 settlements");
    }

    @Test
    void escalatesToHighSeverityWhenDebtIsLarge() {
        var context = TestFixtures.riskContext()
                .totalOwed(new BigDecimal("25000")) // >= 2× the ₹10,000 threshold
                .owedSettlementCount(4)
                .build();

        List<InsightDraft> drafts = rule.evaluate(context);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getSeverity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void raisesMediumRiskOnManySettlementsEvenBelowAmountThreshold() {
        var context = TestFixtures.riskContext()
                .totalOwed(new BigDecimal("4000")) // below ₹10,000 threshold
                .owedSettlementCount(3)            // but hits the settlement-count trigger
                .build();

        List<InsightDraft> drafts = rule.evaluate(context);

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getSeverity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    void noRiskWhenDebtIsSmallAndFewSettlements() {
        var context = TestFixtures.riskContext()
                .totalOwed(new BigDecimal("2000"))
                .owedSettlementCount(1)
                .build();

        assertThat(rule.evaluate(context)).isEmpty();
    }

    @Test
    void noRiskWhenNoDebtData() {
        assertThat(rule.evaluate(TestFixtures.riskContext().build())).isEmpty();
    }
}
