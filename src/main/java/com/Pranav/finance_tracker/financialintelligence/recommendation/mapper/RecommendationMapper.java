package com.Pranav.finance_tracker.financialintelligence.recommendation.mapper;

import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.FinancialRecommendationResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.FinancialRecommendation;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationStatus;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Converts between {@link RecommendationDraft}s, {@link FinancialRecommendation} entities and
 * {@link FinancialRecommendationResponse} DTOs. Keeps mapping out of the service and controller.
 */
@Component
public class RecommendationMapper {

    /**
     * Materializes a draft into a persistable entity in {@link RecommendationStatus#ACTIVE}.
     *
     * @param user  owner of the recommendation
     * @param draft the rule-produced draft
     * @param now   generation timestamp
     * @param ttl   how long the recommendation stays active
     */
    public FinancialRecommendation toEntity(User user, RecommendationDraft draft, LocalDateTime now, Duration ttl) {
        return FinancialRecommendation.builder()
                .userId(user.getId())
                .ruleKey(draft.getRuleKey())
                .title(draft.getTitle())
                .description(draft.getDescription())
                .recommendationType(draft.getRecommendationType())
                .priority(draft.getPriority())
                .expectedMonthlySaving(draft.getExpectedMonthlySaving())
                .confidence(draft.getConfidence())
                .actionText(draft.getActionText())
                .status(RecommendationStatus.ACTIVE)
                .createdAt(now)
                .expiresAt(now.plus(ttl))
                .build();
    }

    public FinancialRecommendationResponse toResponse(FinancialRecommendation entity) {
        return FinancialRecommendationResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .recommendationType(entity.getRecommendationType())
                .priority(entity.getPriority())
                .expectedMonthlySaving(entity.getExpectedMonthlySaving())
                .confidence(entity.getConfidence())
                .actionText(entity.getActionText())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }
}
