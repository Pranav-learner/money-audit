package com.Pranav.finance_tracker.financialintelligence.healthscore.service;

import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScoreResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScorePointResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScoreResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScoreResult;
import com.Pranav.finance_tracker.financialintelligence.healthscore.engine.HealthScoreEngine;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.FinancialHealthScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import com.Pranav.finance_tracker.financialintelligence.healthscore.repository.FinancialHealthScoreRepository;
import com.Pranav.finance_tracker.financialintelligence.notification.InsightNotificationService;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates health-score generation (write side) and retrieval (read side).
 *
 * <p>Generation runs the {@link HealthScoreEngine} over the shared {@link InsightContext}, stores a
 * daily snapshot (append-only history) and notifies the user if the score dropped meaningfully.
 * Reads compute the <b>current</b> score live (so the full, explainable breakdown is always fresh)
 * and compare it against the last stored snapshot to explain the change.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthScoreService {

    private final HealthScoreEngine engine;
    private final InsightContextFactory contextFactory;
    private final FinancialHealthScoreRepository repository;
    private final HealthScoreProperties properties;
    private final InsightNotificationService notificationService;

    // ── Write side (nightly scheduler) ──────────────────────────────────

    @Transactional
    public int generateForUser(User user) {
        return generateForUser(user, contextFactory.build(user));
    }

    /**
     * Evaluates and stores today's health score for the user (once per day) and notifies them on a
     * significant drop versus the previous snapshot.
     *
     * @return the overall score
     */
    @Transactional
    public int generateForUser(User user, InsightContext insightContext) {
        HealthScoreResult result = engine.evaluate(insightContext);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        FinancialHealthScore previous = repository.findFirstByUserIdOrderByCreatedAtDesc(user.getId());

        if (repository.existsByUserIdAndCreatedAtBetween(user.getId(), dayStart, dayEnd)) {
            return result.getOverallScore(); // already recorded today
        }

        repository.save(toEntity(user, result, now));

        if (previous != null) {
            int drop = previous.getScore() - result.getOverallScore();
            if (drop >= properties.getDropNotificationPoints()) {
                notificationService.notifyHealthScore(user.getId(), String.format(
                        "Your Financial Health Score dropped %d points to %d. %s",
                        drop, result.getOverallScore(), result.getExplanation()));
            }
        }
        return result.getOverallScore();
    }

    // ── Read side (REST controller) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public HealthScoreResponse getCurrentScore(User user) {
        HealthScoreResult result = engine.evaluate(contextFactory.build(user));
        FinancialHealthScore previous = repository.findFirstByUserIdOrderByCreatedAtDesc(user.getId());

        Integer change = previous == null ? null : result.getOverallScore() - previous.getScore();
        String changeExplanation = previous == null ? null : explainChange(result, previous, change);

        return HealthScoreResponse.builder()
                .score(result.getOverallScore())
                .band(result.getBand())
                .components(result.getComponents().stream().map(this::toComponentResponse).toList())
                .explanation(result.getExplanation())
                .changeSincePrevious(change)
                .changeExplanation(changeExplanation)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<HealthScorePointResponse> getHistory(User user, int limit) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(s -> HealthScorePointResponse.builder()
                        .score(s.getScore()).band(s.getBand()).createdAt(s.getCreatedAt()).build())
                .toList();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String explainChange(HealthScoreResult current, FinancialHealthScore previous, int change) {
        if (change == 0) {
            return "Your score is unchanged since the last snapshot.";
        }
        Map<HealthComponent, Integer> currentByComponent = current.getComponents().stream()
                .collect(Collectors.toMap(ComponentScore::getComponent, ComponentScore::getScore));
        Map<HealthComponent, Integer> previousByComponent = Map.of(
                HealthComponent.BUDGET_ADHERENCE, previous.getBudgetScore(),
                HealthComponent.SAVINGS_BEHAVIOR, previous.getSavingsScore(),
                HealthComponent.DEBT_MANAGEMENT, previous.getDebtScore(),
                HealthComponent.SPENDING_STABILITY, previous.getSpendingScore());

        HealthComponent biggestMover = null;
        int biggestDelta = 0;
        for (HealthComponent component : currentByComponent.keySet()) {
            int delta = currentByComponent.get(component) - previousByComponent.getOrDefault(component, 0);
            if (Math.abs(delta) > Math.abs(biggestDelta)) {
                biggestDelta = delta;
                biggestMover = component;
            }
        }

        String direction = change > 0 ? "improved" : "declined";
        if (biggestMover == null || biggestDelta == 0) {
            return String.format("Your score %s by %d point(s) since the last snapshot.", direction, Math.abs(change));
        }
        String driver = humanize(biggestMover.name());
        String moverDirection = biggestDelta > 0 ? "stronger" : "weaker";
        return String.format("Your score %s by %d point(s), mainly because %s got %s.",
                direction, Math.abs(change), driver, moverDirection);
    }

    private FinancialHealthScore toEntity(User user, HealthScoreResult result, LocalDateTime now) {
        Map<HealthComponent, ComponentScore> byComponent = result.getComponents().stream()
                .collect(Collectors.toMap(ComponentScore::getComponent, Function.identity()));
        return FinancialHealthScore.builder()
                .userId(user.getId())
                .score(result.getOverallScore())
                .band(result.getBand())
                .budgetScore(scoreOf(byComponent, HealthComponent.BUDGET_ADHERENCE))
                .savingsScore(scoreOf(byComponent, HealthComponent.SAVINGS_BEHAVIOR))
                .debtScore(scoreOf(byComponent, HealthComponent.DEBT_MANAGEMENT))
                .spendingScore(scoreOf(byComponent, HealthComponent.SPENDING_STABILITY))
                .explanation(result.getExplanation())
                .createdAt(now)
                .build();
    }

    private int scoreOf(Map<HealthComponent, ComponentScore> byComponent, HealthComponent component) {
        ComponentScore c = byComponent.get(component);
        return c == null ? 0 : c.getScore();
    }

    private ComponentScoreResponse toComponentResponse(ComponentScore c) {
        return ComponentScoreResponse.builder()
                .component(c.getComponent()).score(c.getScore()).maxPoints(c.getMaxPoints()).reason(c.getReason())
                .build();
    }

    private String humanize(String enumName) {
        return enumName.toLowerCase().replace('_', ' ');
    }
}
