package com.Pranav.finance_tracker.financialintelligence.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregate view powering both the recommendations summary and the dashboard card:
 * totals by status, potential savings, the single highest-priority recommendation, the top few
 * recommendations and the most recently completed ones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSummaryResponse {

    private long totalRecommendations;
    private long activeCount;
    private long completedCount;
    private long dismissedCount;

    /** Sum of expected monthly savings across active recommendations. */
    private BigDecimal potentialMonthlySavings;

    /** {@link #potentialMonthlySavings} × 12. */
    private BigDecimal potentialAnnualSavings;

    /** The top-priority active recommendation, or {@code null} if none. */
    private FinancialRecommendationResponse highestPriority;

    /** Top-N active recommendations (dashboard "Top 3"). */
    private List<FinancialRecommendationResponse> topRecommendations;

    /** Most recently completed recommendations, for positive reinforcement. */
    private List<FinancialRecommendationResponse> recentlyCompleted;
}
