package com.Pranav.finance_tracker.financialintelligence.risk.service;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.RiskRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskDetectionEngineTest {

    private InsightContext context() {
        return TestFixtures.riskContext().build();
    }

    private InsightDraft draft(String key) {
        return InsightDraft.builder()
                .ruleKey(key)
                .title(key)
                .description("desc")
                .insightType(InsightType.SPENDING_WARNING)
                .severity(Severity.MEDIUM)
                .riskType(FinancialRiskType.BUDGET_RISK)
                .confidence(0.8)
                .build();
    }

    private RiskRule stub(String key, FinancialRiskType type, List<InsightDraft> result, boolean explode) {
        return new RiskRule() {
            @Override
            public String ruleKey() {
                return key;
            }

            @Override
            public FinancialRiskType riskType() {
                return type;
            }

            @Override
            public List<InsightDraft> evaluate(InsightContext context) {
                if (explode) {
                    throw new IllegalStateException("boom");
                }
                return result;
            }
        };
    }

    @Test
    void collectsDraftsFromAllRiskRules() {
        RiskRule a = stub("A", FinancialRiskType.BUDGET_RISK, List.of(draft("A1")), false);
        RiskRule b = stub("B", FinancialRiskType.DEBT_RISK, List.of(draft("B1"), draft("B2")), false);

        RiskDetectionEngine engine = new RiskDetectionEngine(List.of(a, b));
        List<InsightDraft> drafts = engine.run(context());

        assertThat(drafts).extracting(InsightDraft::getRuleKey).containsExactly("A1", "B1", "B2");
    }

    @Test
    void isolatesAFailingRiskRule() {
        RiskRule failing = stub("BOOM", FinancialRiskType.CASHFLOW_RISK, List.of(), true);
        RiskRule healthy = stub("OK", FinancialRiskType.SAVINGS_RISK, List.of(draft("OK1")), false);

        RiskDetectionEngine engine = new RiskDetectionEngine(List.of(failing, healthy));
        List<InsightDraft> drafts = engine.run(context());

        assertThat(drafts).extracting(InsightDraft::getRuleKey).containsExactly("OK1");
    }
}
