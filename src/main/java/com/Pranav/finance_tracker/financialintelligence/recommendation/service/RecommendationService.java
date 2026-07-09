package com.Pranav.finance_tracker.financialintelligence.recommendation.service;

import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.exception.ResourceNotFoundException;
import com.Pranav.finance_tracker.financialintelligence.notification.InsightNotificationService;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.FinancialRecommendationResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.engine.RecommendationEngine;
import com.Pranav.finance_tracker.financialintelligence.recommendation.engine.RecommendationPriorityEngine;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.FinancialRecommendation;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationStatus;
import com.Pranav.finance_tracker.financialintelligence.recommendation.mapper.RecommendationMapper;
import com.Pranav.finance_tracker.financialintelligence.recommendation.repository.FinancialRecommendationRepository;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates recommendation generation (write side) and retrieval (read side).
 *
 * <p>Generation composes a {@link RecommendationContext} on top of the shared, preloaded
 * {@link InsightContext} (so no data is re-queried), runs the {@link RecommendationEngine},
 * prioritizes and filters the drafts, de-duplicates against what already exists for the day,
 * persists the survivors as append-only history and notifies the user when high-priority
 * opportunities appear. Reads power the REST API and the dashboard summary.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final FinancialRecommendationRepository recommendationRepository;
    private final InsightContextFactory contextFactory;
    private final RecommendationEngine recommendationEngine;
    private final RecommendationPriorityEngine priorityEngine;
    private final HealthScoreProvider healthScoreProvider;
    private final RecommendationMapper mapper;
    private final AnalyticsService analyticsService;
    private final InsightNotificationService notificationService;
    private final RecommendationProperties properties;

    // ── Write side (invoked by the nightly scheduler) ───────────────────

    /** Convenience entry point that builds the shared context itself. */
    @Transactional
    public int generateForUser(User user) {
        return generateForUser(user, contextFactory.build(user));
    }

    /**
     * Generates, prioritizes, de-duplicates and persists today's recommendations for one user,
     * reusing the supplied {@link InsightContext} so the nightly pipeline loads a user's data once.
     *
     * @return the number of newly persisted recommendations
     */
    @Transactional
    public int generateForUser(User user, InsightContext insightContext) {
        RecommendationContext context = buildContext(user, insightContext);

        List<RecommendationDraft> drafts = recommendationEngine.generate(context).stream()
                .filter(d -> d.getConfidence() >= properties.getConfidenceThreshold())
                .filter(d -> priorityEngine.score(d, context) >= properties.getPriorityThreshold())
                .toList();

        List<RecommendationDraft> prioritized = priorityEngine.prioritize(drafts, context).stream()
                .limit(properties.getMaxRecommendationsPerUser())
                .toList();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        Duration ttl = Duration.ofDays(properties.getExpirationDays());

        Set<String> seenThisRun = new HashSet<>();
        List<FinancialRecommendation> toSave = new ArrayList<>();
        for (RecommendationDraft draft : prioritized) {
            if (!seenThisRun.add(draft.getRuleKey())) {
                continue;
            }
            boolean alreadyToday = recommendationRepository.existsByUserIdAndRuleKeyAndCreatedAtBetween(
                    user.getId(), draft.getRuleKey(), dayStart, dayEnd);
            if (alreadyToday) {
                continue;
            }
            toSave.add(mapper.toEntity(user, draft, now, ttl));
        }

        if (toSave.isEmpty()) {
            return 0;
        }

        recommendationRepository.saveAll(toSave);
        notifyIfHighValue(user.getId(), toSave);
        return toSave.size();
    }

    private RecommendationContext buildContext(User user, InsightContext insightContext) {
        int healthScore = healthScoreProvider.scoreFor(insightContext);
        BigDecimal totalSavings = safe(analyticsService.getTotalSavings(user).getTotalSavings());
        BigDecimal savedThisMonth = safe(analyticsService
                .getMonthlySavings(user, insightContext.getCurrentMonth().getMonthValue(),
                        insightContext.getCurrentMonth().getYear())
                .getTotalSaved());

        return RecommendationContext.builder()
                .insight(insightContext)
                .healthScore(healthScore)
                .totalSavings(totalSavings)
                .savedThisMonth(savedThisMonth)
                .build();
    }

    private void notifyIfHighValue(UUID userId, List<FinancialRecommendation> saved) {
        boolean hasHighPriority = saved.stream()
                .anyMatch(r -> r.getPriority() == Priority.HIGH || r.getPriority() == Priority.CRITICAL);
        if (!hasHighPriority) {
            return;
        }
        BigDecimal totalMonthlySaving = saved.stream()
                .map(FinancialRecommendation::getExpectedMonthlySaving)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        notificationService.notifyRecommendations(userId, saved.size(), totalMonthlySaving);
    }

    // ── Read side (invoked by the REST controller) ──────────────────────

    @Transactional(readOnly = true)
    public List<FinancialRecommendationResponse> getActiveRecommendations(User user) {
        return activeRecommendations(user).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FinancialRecommendationResponse> getTopRecommendations(User user) {
        return activeRecommendations(user).stream()
                .limit(properties.getTopCount())
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinancialRecommendationResponse> getHistory(User user) {
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public FinancialRecommendationResponse dismiss(User user, UUID id) {
        FinancialRecommendation recommendation = requireOwned(user, id);
        recommendation.setStatus(RecommendationStatus.DISMISSED);
        return mapper.toResponse(recommendation);
    }

    @Transactional
    public FinancialRecommendationResponse complete(User user, UUID id) {
        FinancialRecommendation recommendation = requireOwned(user, id);
        recommendation.setStatus(RecommendationStatus.COMPLETED);
        return mapper.toResponse(recommendation);
    }

    @Transactional(readOnly = true)
    public RecommendationSummaryResponse getSummary(User user) {
        List<FinancialRecommendation> active = activeRecommendations(user);

        BigDecimal monthlySavings = active.stream()
                .map(FinancialRecommendation::getExpectedMonthlySaving)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FinancialRecommendationResponse> top = active.stream()
                .limit(properties.getTopCount())
                .map(mapper::toResponse)
                .toList();

        List<FinancialRecommendationResponse> recentlyCompleted = recommendationRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), RecommendationStatus.COMPLETED).stream()
                .limit(properties.getTopCount())
                .map(mapper::toResponse)
                .toList();

        return RecommendationSummaryResponse.builder()
                .totalRecommendations(recommendationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size())
                .activeCount(active.size())
                .completedCount(recommendationRepository.countByUserIdAndStatus(user.getId(), RecommendationStatus.COMPLETED))
                .dismissedCount(recommendationRepository.countByUserIdAndStatus(user.getId(), RecommendationStatus.DISMISSED))
                .potentialMonthlySavings(monthlySavings)
                .potentialAnnualSavings(monthlySavings.multiply(BigDecimal.valueOf(12)))
                .highestPriority(top.isEmpty() ? null : top.get(0))
                .topRecommendations(top)
                .recentlyCompleted(recentlyCompleted)
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /** Active recommendations already ordered by descending stored priority, then recency. */
    private List<FinancialRecommendation> activeRecommendations(User user) {
        List<FinancialRecommendation> active = recommendationRepository
                .findByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        user.getId(), RecommendationStatus.ACTIVE, LocalDateTime.now());
        active.sort((a, b) -> {
            int byPriority = b.getPriority().ordinal() - a.getPriority().ordinal();
            if (byPriority != 0) {
                return byPriority;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return active;
    }

    private FinancialRecommendation requireOwned(User user, UUID id) {
        return recommendationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + id));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
