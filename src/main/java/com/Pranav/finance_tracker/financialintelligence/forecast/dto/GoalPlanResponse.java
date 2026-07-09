package com.Pranav.finance_tracker.financialintelligence.forecast.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A complete, actionable financial plan for reaching a goal — the output of the
 * {@code FinancialPlanningEngine}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalPlanResponse {

    private UUID goalId;
    private String goalTitle;

    /** Rule 1 — the monthly saving needed to hit the target by the target date. */
    private BigDecimal requiredMonthlySaving;

    /** Rule 2 — the user's current estimated monthly saving capacity. */
    private BigDecimal currentMonthlyCapacity;

    /** Rule 3 — estimated probability (0–1) of reaching the goal on time. */
    private double successProbability;

    /** Whether the goal is feasible on the current target date. */
    private boolean feasible;

    /** Rule 4 — concrete expense reductions / savings increases that close the gap. */
    private List<PlanAction> recommendedActions;

    /** Projected completion date at the user's current saving capacity. */
    private LocalDate projectedCompletionDate;

    /** Rule 5 / Rule 6 — a realistic target date when the current one is not achievable. */
    private LocalDate alternativeTargetDate;

    private int monthsRemaining;

    /** Human-readable summary of the plan (or the impossible-goal advice). */
    private String summary;
}
