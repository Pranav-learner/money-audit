package com.Pranav.finance_tracker.financialintelligence.recommendation.dto;

import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationStatus;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Client-facing view of a {@code FinancialRecommendation}. Entities are never exposed directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialRecommendationResponse {

    private UUID id;
    private String title;
    private String description;
    private RecommendationType recommendationType;
    private Priority priority;
    private BigDecimal expectedMonthlySaving;
    private double confidence;
    private String actionText;
    private RecommendationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
