package com.Pranav.finance_tracker.financialintelligence.mapper;

import com.Pranav.finance_tracker.financialintelligence.dto.FinancialInsightResponse;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.FinancialInsight;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Converts between rule {@link InsightDraft}s, {@link FinancialInsight} entities and
 * {@link FinancialInsightResponse} DTOs. Keeps mapping out of the service and controller.
 */
@Component
public class InsightMapper {

    /**
     * Materializes a draft into a persistable entity.
     *
     * @param user  owner of the insight
     * @param draft the rule-produced draft
     * @param now   generation timestamp
     * @param ttl   how long the insight stays active
     */
    public FinancialInsight toEntity(User user, InsightDraft draft, LocalDateTime now, Duration ttl) {
        return FinancialInsight.builder()
                .userId(user.getId())
                .ruleKey(draft.getRuleKey())
                .title(draft.getTitle())
                .description(draft.getDescription())
                .insightType(draft.getInsightType())
                .severity(draft.getSeverity())
                .riskType(draft.getRiskType())
                .category(draft.getCategory())
                .actionSuggestion(draft.getActionSuggestion())
                .confidence(draft.getConfidence())
                .createdAt(now)
                .expiresAt(now.plus(ttl))
                .viewed(false)
                .dismissed(false)
                .build();
    }

    public FinancialInsightResponse toResponse(FinancialInsight entity) {
        return FinancialInsightResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .insightType(entity.getInsightType())
                .severity(entity.getSeverity())
                .riskType(entity.getRiskType())
                .category(entity.getCategory())
                .actionSuggestion(entity.getActionSuggestion())
                .confidence(entity.getConfidence())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .viewed(entity.isViewed())
                .dismissed(entity.isDismissed())
                .build();
    }
}
