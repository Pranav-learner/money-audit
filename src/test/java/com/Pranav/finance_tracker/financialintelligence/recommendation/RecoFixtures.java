package com.Pranav.finance_tracker.financialintelligence.recommendation;

import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;

import java.math.BigDecimal;

/**
 * Shared builders for recommendation unit tests.
 */
public final class RecoFixtures {

    private RecoFixtures() {
    }

    public static RecommendationContext context(InsightContext insight, int healthScore,
                                                String totalSavings, String savedThisMonth) {
        return RecommendationContext.builder()
                .insight(insight)
                .healthScore(healthScore)
                .totalSavings(new BigDecimal(totalSavings))
                .savedThisMonth(new BigDecimal(savedThisMonth))
                .build();
    }

    public static RecommendationDraft draft(String key, RecommendationType type, Priority priority,
                                            String monthlySaving, double confidence) {
        return RecommendationDraft.builder()
                .ruleKey(key)
                .title(key)
                .description("desc")
                .recommendationType(type)
                .priority(priority)
                .expectedMonthlySaving(new BigDecimal(monthlySaving))
                .confidence(confidence)
                .actionText("do it")
                .build();
    }
}
