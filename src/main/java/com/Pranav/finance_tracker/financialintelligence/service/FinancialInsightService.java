package com.Pranav.finance_tracker.financialintelligence.service;

import com.Pranav.finance_tracker.exception.ResourceNotFoundException;
import com.Pranav.finance_tracker.financialintelligence.dto.FinancialInsightResponse;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.entity.FinancialInsight;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.mapper.InsightMapper;
import com.Pranav.finance_tracker.financialintelligence.notification.InsightNotificationService;
import com.Pranav.finance_tracker.financialintelligence.repository.FinancialInsightRepository;
import com.Pranav.finance_tracker.financialintelligence.risk.service.RiskDetectionEngine;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightEngine;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates insight generation (write side) and retrieval (read side).
 *
 * <p>Generation builds a per-user {@link InsightContext}, runs the {@link InsightEngine},
 * de-duplicates the resulting drafts against what already exists for the day, persists the
 * survivors and — if any were new — triggers a single in-app notification.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialInsightService {

    /** How long a generated insight remains "active" before it expires. */
    private static final Duration INSIGHT_TTL = Duration.ofDays(14);

    private final FinancialInsightRepository insightRepository;
    private final InsightContextFactory contextFactory;
    private final InsightEngine insightEngine;
    private final RiskDetectionEngine riskDetectionEngine;
    private final InsightMapper insightMapper;
    private final InsightNotificationService notificationService;

    // ── Write side (invoked by the nightly scheduler) ───────────────────

    /**
     * Generates, de-duplicates and persists today's insights for a single user, then notifies
     * them if anything new was produced.
     *
     * @return the number of newly persisted insights
     */
    /** Convenience entry point that builds the context itself. */
    @Transactional
    public int generateForUser(User user) {
        return generateForUser(user, contextFactory.build(user));
    }

    /**
     * Generates insights for a user from an already-built {@link InsightContext}. The nightly
     * scheduler uses this overload so a user's financial data is loaded once and shared across the
     * spending-intelligence, risk and recommendation phases.
     *
     * @return the number of newly persisted insights
     */
    @Transactional
    public int generateForUser(User user, InsightContext context) {
        // Execution flow: Spending Intelligence → Risk Detection → persist → notify.
        // Both phases share the one preloaded context, so risk detection adds no extra scans.
        List<InsightDraft> drafts = new ArrayList<>();
        drafts.addAll(insightEngine.run(context));
        drafts.addAll(riskDetectionEngine.run(context));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        Set<String> seenThisRun = new HashSet<>();
        List<FinancialInsight> toSave = new ArrayList<>();

        for (InsightDraft draft : drafts) {
            // Guard against duplicate keys within this run and against ones already stored today.
            if (!seenThisRun.add(draft.getRuleKey())) {
                continue;
            }
            boolean alreadyToday = insightRepository.existsByUserIdAndRuleKeyAndCreatedAtBetween(
                    user.getId(), draft.getRuleKey(), dayStart, dayEnd);
            if (alreadyToday) {
                continue;
            }
            toSave.add(insightMapper.toEntity(user, draft, now, INSIGHT_TTL));
        }

        if (toSave.isEmpty()) {
            return 0;
        }

        insightRepository.saveAll(toSave);
        notificationService.notifyInsightsReady(user.getId(), toSave.size());

        // Escalate: if any newly-generated HIGH-severity risks exist, raise a dedicated risk alert.
        long highSeverityRisks = toSave.stream()
                .filter(i -> i.getRiskType() != null)
                .filter(i -> i.getSeverity() == Severity.HIGH)
                .count();
        if (highSeverityRisks > 0) {
            notificationService.notifyRisksDetected(user.getId(), (int) highSeverityRisks);
        }
        return toSave.size();
    }

    // ── Read side (invoked by the REST controller) ──────────────────────

    @Transactional(readOnly = true)
    public List<FinancialInsightResponse> getActiveInsights(User user) {
        return insightRepository
                .findByUserIdAndDismissedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user.getId(), LocalDateTime.now())
                .stream()
                .map(insightMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinancialInsightResponse> getUnreadInsights(User user) {
        return insightRepository
                .findByUserIdAndDismissedFalseAndViewedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        user.getId(), LocalDateTime.now())
                .stream()
                .map(insightMapper::toResponse)
                .toList();
    }

    @Transactional
    public FinancialInsightResponse markAsRead(User user, UUID insightId) {
        FinancialInsight insight = requireOwned(user, insightId);
        insight.setViewed(true);
        return insightMapper.toResponse(insight);
    }

    @Transactional
    public FinancialInsightResponse dismiss(User user, UUID insightId) {
        FinancialInsight insight = requireOwned(user, insightId);
        insight.setDismissed(true);
        return insightMapper.toResponse(insight);
    }

    @Transactional(readOnly = true)
    public InsightSummaryResponse getSummary(User user) {
        List<FinancialInsight> active = insightRepository
                .findByUserIdAndDismissedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user.getId(), LocalDateTime.now());

        long unread = active.stream().filter(i -> !i.isViewed()).count();
        long highSeverity = active.stream().filter(i -> i.getSeverity() == Severity.HIGH).count();
        FinancialInsightResponse latest = active.isEmpty() ? null : insightMapper.toResponse(active.get(0));

        return InsightSummaryResponse.builder()
                .totalInsights(active.size())
                .unreadCount(unread)
                .highSeverityCount(highSeverity)
                .latestInsight(latest)
                .build();
    }

    private FinancialInsight requireOwned(User user, UUID insightId) {
        return insightRepository.findByIdAndUserId(insightId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Insight not found: " + insightId));
    }
}
