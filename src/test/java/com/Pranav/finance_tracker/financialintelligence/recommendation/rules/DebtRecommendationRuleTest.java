package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.DebtRecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DebtRecommendationRuleTest {

    private final DebtRecommendationRule rule = new DebtRecommendationRule();

    @Test
    void recommendsSettlementPriorityWhenDebtHigh() {
        InsightContext insight = TestFixtures.riskContext()
                .totalOwed(new BigDecimal("8000"))
                .owedSettlementCount(4)
                .build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 40, "0", "0"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRecommendationType()).isEqualTo(RecommendationType.DEBT);
        assertThat(drafts.get(0).getPriority()).isEqualTo(Priority.HIGH);
        assertThat(drafts.get(0).getDescription()).contains("₹8,000").contains("4 settlements");
    }

    @Test
    void escalatesToCriticalForLargeDebt() {
        InsightContext insight = TestFixtures.riskContext()
                .totalOwed(new BigDecimal("15000"))
                .owedSettlementCount(2)
                .build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 30, "0", "0")).get(0).getPriority())
                .isEqualTo(Priority.CRITICAL);
    }

    @Test
    void noRecommendationWhenDebtLow() {
        InsightContext insight = TestFixtures.riskContext()
                .totalOwed(new BigDecimal("1000"))
                .owedSettlementCount(1)
                .build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 80, "0", "0"))).isEmpty();
    }
}
