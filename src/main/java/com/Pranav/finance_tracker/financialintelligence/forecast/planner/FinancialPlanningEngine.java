package com.Pranav.finance_tracker.financialintelligence.forecast.planner;

import com.Pranav.finance_tracker.financialintelligence.forecast.config.ForecastProperties;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalPlanResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.PlanAction;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.FinancialGoal;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.engine.RecommendationEngine;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns a {@link FinancialGoal} into a concrete, explainable financial plan.
 *
 * <p>The engine composes six planning rules (each a clearly-named step) using the shared
 * {@link ForecastContext} for the user's saving capacity and <b>reusing the Recommendation
 * Engine</b> to source concrete expense reductions rather than duplicating that logic:</p>
 * <ol>
 *   <li>Rule 1 — required monthly contribution.</li>
 *   <li>Rule 2 — compare against current saving capacity.</li>
 *   <li>Rule 3 — estimate probability of success.</li>
 *   <li>Rule 4 — recommend expense reductions to close any gap.</li>
 *   <li>Rule 5 — suggest a realistic target date.</li>
 *   <li>Rule 6 — detect impossible goals and advise extending the timeline.</li>
 * </ol>
 *
 * <p>The engine is a pure function of (goal, context); it performs no persistence, which keeps it
 * simple to test and open to future reinforcement-learning / optimisation replacements.</p>
 */
@Component
@RequiredArgsConstructor
public class FinancialPlanningEngine {

    private final RecommendationEngine recommendationEngine;
    private final ForecastProperties properties;

    public GoalPlanResponse buildPlan(FinancialGoal goal, ForecastContext context) {
        LocalDate today = context.today();
        int monthsRemaining = monthsBetween(today, goal.getTargetDate());
        BigDecimal remaining = goal.getTargetAmount().subtract(nz(goal.getCurrentAmount())).max(BigDecimal.ZERO);

        // Already achieved.
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return GoalPlanResponse.builder()
                    .goalId(goal.getId()).goalTitle(goal.getTitle())
                    .requiredMonthlySaving(BigDecimal.ZERO)
                    .currentMonthlyCapacity(capacity(context))
                    .successProbability(1.0).feasible(true)
                    .recommendedActions(List.of())
                    .projectedCompletionDate(today).monthsRemaining(monthsRemaining)
                    .summary("You've already reached this goal. Consider marking it complete.")
                    .build();
        }

        // Rule 1 — required monthly contribution.
        BigDecimal requiredMonthly = remaining.divide(BigDecimal.valueOf(monthsRemaining), 2, RoundingMode.HALF_UP);

        // Rule 2 — current saving capacity.
        BigDecimal capacity = capacity(context);

        // Rule 3 — probability of success.
        double probability = successProbability(capacity, requiredMonthly, context.getHealthScore(), remaining);

        // Rule 5 — projected completion at current capacity, and a realistic date if needed.
        Integer monthsToComplete = capacity.compareTo(BigDecimal.ZERO) > 0
                ? (int) Math.ceil(remaining.doubleValue() / capacity.doubleValue())
                : null;
        LocalDate projectedCompletion = monthsToComplete == null ? null : today.plusMonths(monthsToComplete);

        // Rule 6 — feasibility / impossible-goal detection.
        boolean feasible = capacity.compareTo(requiredMonthly) >= 0;
        LocalDate alternativeDate = null;
        if (!feasible && monthsToComplete != null) {
            int cappedMonths = Math.min(monthsToComplete, properties.getMaxGoalDurationMonths());
            alternativeDate = today.plusMonths(cappedMonths);
        }

        // Rule 4 — concrete actions to close the gap.
        List<PlanAction> actions = buildActions(goal, context, requiredMonthly, capacity);

        String summary = summarise(goal, feasible, requiredMonthly, capacity, monthsToComplete, alternativeDate, actions);

        return GoalPlanResponse.builder()
                .goalId(goal.getId()).goalTitle(goal.getTitle())
                .requiredMonthlySaving(requiredMonthly)
                .currentMonthlyCapacity(capacity)
                .successProbability(round2(probability))
                .feasible(feasible)
                .recommendedActions(actions)
                .projectedCompletionDate(projectedCompletion)
                .alternativeTargetDate(alternativeDate)
                .monthsRemaining(monthsRemaining)
                .summary(summary)
                .build();
    }

    // ── Rule 2: capacity ────────────────────────────────────────────────
    private BigDecimal capacity(ForecastContext context) {
        BigDecimal projected = context.projectedMonthEndSavings();
        return projected.max(BigDecimal.ZERO);
    }

    // ── Rule 3: probability ─────────────────────────────────────────────
    private double successProbability(BigDecimal capacity, BigDecimal required, int healthScore, BigDecimal remaining) {
        if (required.compareTo(BigDecimal.ZERO) <= 0) {
            return 1.0;
        }
        double base = capacity.compareTo(BigDecimal.ZERO) <= 0
                ? 0.0
                : Math.min(1.0, capacity.doubleValue() / required.doubleValue());
        double health = Math.max(0, Math.min(100, healthScore)) / 100.0;
        double blended = 0.7 * base + 0.3 * health;
        return Math.max(0.0, Math.min(0.99, blended));
    }

    // ── Rule 4: reductions (reuses the Recommendation Engine) ───────────
    private List<PlanAction> buildActions(FinancialGoal goal, ForecastContext context,
                                          BigDecimal requiredMonthly, BigDecimal capacity) {
        List<PlanAction> actions = new ArrayList<>();
        BigDecimal gap = requiredMonthly.subtract(capacity);

        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            // Feasible: a single straightforward action.
            actions.add(PlanAction.builder()
                    .label("Save toward '" + goal.getTitle() + "'")
                    .monthlyAmount(requiredMonthly)
                    .build());
            return actions;
        }

        // Source concrete reductions from the Recommendation Engine, largest first.
        RecommendationContext recoContext = RecommendationContext.builder()
                .insight(context.getInsight())
                .healthScore(context.getHealthScore())
                .totalSavings(context.getTotalSavings())
                .savedThisMonth(context.getSavedThisMonth())
                .build();

        List<RecommendationDraft> savers = recommendationEngine.generate(recoContext).stream()
                .filter(d -> d.getExpectedMonthlySaving() != null
                        && d.getExpectedMonthlySaving().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(RecommendationDraft::getExpectedMonthlySaving).reversed())
                .toList();

        BigDecimal covered = BigDecimal.ZERO;
        for (RecommendationDraft saver : savers) {
            if (covered.compareTo(gap) >= 0) {
                break;
            }
            actions.add(PlanAction.builder()
                    .label(saver.getTitle())
                    .monthlyAmount(saver.getExpectedMonthlySaving())
                    .build());
            covered = covered.add(saver.getExpectedMonthlySaving());
        }

        // Any remaining gap becomes an explicit "save more" action.
        BigDecimal shortfall = gap.subtract(covered);
        if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
            actions.add(PlanAction.builder()
                    .label("Increase monthly savings")
                    .monthlyAmount(shortfall.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return actions;
    }

    // ── Rule 6: summary / impossible-goal advice ────────────────────────
    private String summarise(FinancialGoal goal, boolean feasible, BigDecimal required, BigDecimal capacity,
                             Integer monthsToComplete, LocalDate alternativeDate, List<PlanAction> actions) {
        if (feasible) {
            return String.format("Save %s a month to reach '%s' by %s.",
                    MoneyFormatter.rupees(required), goal.getTitle(), goal.getTargetDate());
        }
        if (capacity.compareTo(BigDecimal.ZERO) <= 0 || monthsToComplete == null) {
            return String.format("Your current saving pattern is unlikely to achieve this goal. "
                    + "Start setting money aside, or extend the target date.");
        }
        return String.format("Your current saving pattern is unlikely to achieve this goal. "
                        + "Consider extending the target date to approximately %d months, or free up %s/month "
                        + "through the suggested actions.",
                monthsToComplete, MoneyFormatter.rupees(required.subtract(capacity).max(BigDecimal.ZERO)));
    }

    private int monthsBetween(LocalDate from, LocalDate to) {
        long months = ChronoUnit.MONTHS.between(from.withDayOfMonth(1), to.withDayOfMonth(1));
        return (int) Math.max(1, months);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
