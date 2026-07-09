package com.Pranav.finance_tracker.financialintelligence.dto;

import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Client-facing view of a {@link com.Pranav.finance_tracker.financialintelligence.entity.FinancialInsight}.
 * Entities are never exposed directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialInsightResponse {

    private UUID id;
    private String title;
    private String description;
    private InsightType insightType;
    private Severity severity;
    private String category;
    private String actionSuggestion;
    private double confidence;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean viewed;
    private boolean dismissed;
}
