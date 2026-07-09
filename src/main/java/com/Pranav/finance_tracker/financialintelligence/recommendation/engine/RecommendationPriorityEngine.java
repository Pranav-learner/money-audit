package com.Pranav.finance_tracker.financialintelligence.recommendation.engine;

import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks recommendation drafts so the highest-value advice surfaces first.
 *
 * <p>Each draft gets a composite 0–100 score blending the signals the spec calls for:</p>
 * <ul>
 *   <li><b>Expected savings</b> — larger monthly savings rank higher.</li>
 *   <li><b>Urgency</b> — the rule's {@code Priority} (LOW…CRITICAL).</li>
 *   <li><b>Confidence</b> — how sure the rule is.</li>
 *   <li><b>Financial health</b> — the lower the user's health score, the more they need help.</li>
 *   <li><b>Financial risk</b> — outstanding debt boosts urgency.</li>
 * </ul>
 *
 * <p>The engine is a pure function of its inputs (no side effects), which keeps it trivially
 * testable and swappable — a learned ranking model could replace the weighting later.</p>
 */
@Component
public class RecommendationPriorityEngine {

    // Weights sum to 1.0.
    private static final double W_SAVINGS = 0.30;
    private static final double W_URGENCY = 0.25;
    private static final double W_CONFIDENCE = 0.20;
    private static final double W_HEALTH = 0.15;
    private static final double W_RISK = 0.10;

    /** Monthly saving (₹) treated as "maximal" when normalising the savings signal. */
    private static final double SAVINGS_CAP = 5000.0;

    /** Outstanding debt (₹) treated as "maximal" when normalising the risk signal. */
    private static final double DEBT_CAP = 20000.0;

    /**
     * Returns the drafts ordered by descending composite score. The input list is not mutated.
     */
    public List<RecommendationDraft> prioritize(List<RecommendationDraft> drafts, RecommendationContext context) {
        return drafts.stream()
                .sorted(Comparator.comparingDouble((RecommendationDraft d) -> score(d, context)).reversed())
                .toList();
    }

    /** Composite score in [0, 100]; exposed for transparency and testing. */
    public double score(RecommendationDraft draft, RecommendationContext context) {
        double savings = normalise(amount(draft.getExpectedMonthlySaving()), SAVINGS_CAP);
        double urgency = draft.getPriority() == null ? 0 : draft.getPriority().ordinal() / 3.0;
        double confidence = clamp01(draft.getConfidence());
        double healthUrgency = (100 - clampScore(context.getHealthScore())) / 100.0;
        double risk = normalise(amount(context.getInsight().getTotalOwed()), DEBT_CAP);

        double weighted = W_SAVINGS * savings
                + W_URGENCY * urgency
                + W_CONFIDENCE * confidence
                + W_HEALTH * healthUrgency
                + W_RISK * risk;
        return 100.0 * weighted;
    }

    private double amount(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private double normalise(double value, double cap) {
        if (value <= 0) {
            return 0.0;
        }
        return Math.min(1.0, value / cap);
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
