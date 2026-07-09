package com.Pranav.finance_tracker.financialintelligence.forecast.service;

import com.Pranav.finance_tracker.exception.ResourceNotFoundException;
import com.Pranav.finance_tracker.financialintelligence.forecast.config.ForecastProperties;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.*;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.FinancialGoal;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalStatus;
import com.Pranav.finance_tracker.financialintelligence.forecast.planner.FinancialPlanningEngine;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.forecast.repository.FinancialGoalRepository;
import com.Pranav.finance_tracker.financialintelligence.notification.InsightNotificationService;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages financial goals: CRUD, on-demand plans/forecasts, and the nightly goal analysis that
 * refreshes derived fields and raises progress/schedule notifications.
 *
 * <p>All planning maths is delegated to the {@link FinancialPlanningEngine} (which itself reuses the
 * Recommendation Engine), so this service only orchestrates persistence, mapping and notification.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoalService {

    private final FinancialGoalRepository goalRepository;
    private final ForecastContextFactory contextFactory;
    private final FinancialPlanningEngine planningEngine;
    private final InsightNotificationService notificationService;
    private final ForecastProperties properties;

    // ── CRUD ────────────────────────────────────────────────────────────

    @Transactional
    public FinancialGoalResponse createGoal(User user, GoalRequest request) {
        LocalDateTime now = LocalDateTime.now();
        FinancialGoal goal = FinancialGoal.builder()
                .userId(user.getId())
                .title(request.getTitle())
                .goalType(request.getGoalType())
                .targetAmount(request.getTargetAmount())
                .currentAmount(request.getCurrentAmount() == null ? BigDecimal.ZERO : request.getCurrentAmount())
                .targetDate(request.getTargetDate())
                .status(GoalStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        applyDerivedFields(user, goal);
        goalRepository.save(goal);
        return toResponse(goal);
    }

    @Transactional
    public FinancialGoalResponse updateGoal(User user, UUID id, GoalRequest request) {
        FinancialGoal goal = requireOwned(user, id);
        goal.setTitle(request.getTitle());
        goal.setGoalType(request.getGoalType());
        goal.setTargetAmount(request.getTargetAmount());
        if (request.getCurrentAmount() != null) {
            goal.setCurrentAmount(request.getCurrentAmount());
        }
        goal.setTargetDate(request.getTargetDate());
        goal.setUpdatedAt(LocalDateTime.now());

        applyDerivedFields(user, goal);
        return toResponse(goal);
    }

    @Transactional
    public void deleteGoal(User user, UUID id) {
        FinancialGoal goal = requireOwned(user, id);
        goalRepository.delete(goal);
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalResponse> getActiveGoals(User user) {
        return goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), GoalStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    // ── Plans & forecasts ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GoalPlanResponse getPlan(User user, UUID id) {
        FinancialGoal goal = requireOwned(user, id);
        return planningEngine.buildPlan(goal, contextFactory.build(user));
    }

    @Transactional(readOnly = true)
    public GoalForecastResponse getGoalForecast(User user, UUID id) {
        FinancialGoal goal = requireOwned(user, id);
        GoalPlanResponse plan = planningEngine.buildPlan(goal, contextFactory.build(user));

        List<String> recommendations = new ArrayList<>();
        if (plan.getRecommendedActions() != null) {
            plan.getRecommendedActions().forEach(a -> recommendations.add(
                    a.getLabel() + " — ₹" + a.getMonthlyAmount().stripTrailingZeros().toPlainString() + "/month"));
        }
        if (!plan.isFeasible() && plan.getAlternativeTargetDate() != null) {
            recommendations.add("Consider extending the target date to " + plan.getAlternativeTargetDate() + ".");
        }

        return GoalForecastResponse.builder()
                .goalId(goal.getId())
                .successProbability(plan.getSuccessProbability())
                .projectedCompletionDate(plan.getProjectedCompletionDate())
                .requiredMonthlySaving(plan.getRequiredMonthlySaving())
                .recommendations(recommendations)
                .build();
    }

    // ── Nightly analysis ────────────────────────────────────────────────

    /**
     * Refreshes derived fields and raises at most one notification per active goal.
     *
     * @return the number of goals analysed
     */
    @Transactional
    public int analyzeGoals(User user, InsightContext insightContext) {
        List<FinancialGoal> goals = goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), GoalStatus.ACTIVE);
        if (goals.isEmpty()) {
            return 0;
        }
        ForecastContext context = contextFactory.build(user, insightContext);

        for (FinancialGoal goal : goals) {
            GoalPlanResponse plan = planningEngine.buildPlan(goal, context);
            goal.setMonthlyContributionRequired(plan.getRequiredMonthlySaving());
            goal.setProjectedCompletionDate(plan.getProjectedCompletionDate());
            goal.setUpdatedAt(LocalDateTime.now());
            notifyForGoal(user, goal, plan);
        }
        return goals.size();
    }

    private void notifyForGoal(User user, FinancialGoal goal, GoalPlanResponse plan) {
        // Completed?
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
            notificationService.notifyGoalUpdate(user.getId(),
                    String.format("Goal complete! You've reached your %s goal.", goal.getTitle()));
            return;
        }

        LocalDate projected = plan.getProjectedCompletionDate();
        if (projected != null) {
            long delayMonths = ChronoUnit.MONTHS.between(goal.getTargetDate().withDayOfMonth(1), projected.withDayOfMonth(1));
            if (delayMonths >= properties.getGoalDelayNotificationMonths()) {
                notificationService.notifyGoalUpdate(user.getId(), String.format(
                        "At your current spending rate your %s goal may be delayed by %d months.",
                        goal.getTitle(), delayMonths));
                return;
            }
            if (delayMonths <= -1) {
                notificationService.notifyGoalUpdate(user.getId(),
                        String.format("You're ahead of schedule for your %s goal.", goal.getTitle()));
                return;
            }
        }

        int progress = progressPercent(goal);
        if (progress >= (int) Math.round(properties.getNotificationProgressThreshold() * 100)) {
            notificationService.notifyGoalUpdate(user.getId(), String.format(
                    "Excellent progress! Your %s is %d%% complete.", goal.getTitle(), progress));
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void applyDerivedFields(User user, FinancialGoal goal) {
        GoalPlanResponse plan = planningEngine.buildPlan(goal, contextFactory.build(user));
        goal.setMonthlyContributionRequired(plan.getRequiredMonthlySaving());
        goal.setProjectedCompletionDate(plan.getProjectedCompletionDate());
    }

    private int progressPercent(FinancialGoal goal) {
        if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int pct = goal.getCurrentAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(0, Math.min(100, pct));
    }

    private FinancialGoal requireOwned(User user, UUID id) {
        return goalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + id));
    }

    private FinancialGoalResponse toResponse(FinancialGoal goal) {
        return FinancialGoalResponse.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .goalType(goal.getGoalType())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .monthlyContributionRequired(goal.getMonthlyContributionRequired())
                .projectedCompletionDate(goal.getProjectedCompletionDate())
                .status(goal.getStatus())
                .progressPercent(progressPercent(goal))
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
