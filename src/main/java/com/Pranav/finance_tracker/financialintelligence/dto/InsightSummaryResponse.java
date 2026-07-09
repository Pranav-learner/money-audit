package com.Pranav.finance_tracker.financialintelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate view for the AI Insights page header:
 * total active insights, unread count, high-severity count and the latest insight.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightSummaryResponse {

    private long totalInsights;
    private long unreadCount;
    private long highSeverityCount;

    /** Most recently generated active insight, or {@code null} if the user has none. */
    private FinancialInsightResponse latestInsight;
}
