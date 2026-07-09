package com.Pranav.finance_tracker.financialintelligence.forecast.planner;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.ForecastFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.config.ForecastProperties;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalPlanResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.FinancialGoal;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalStatus;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.recommendation.engine.RecommendationEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialPlanningEngineTest {

    @Mock private RecommendationEngine recommendationEngine;

    private final ForecastProperties properties = new ForecastProperties();

    /** Last day of a 31-day month, so saving capacity == savedThisMonth. */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);

    private FinancialPlanningEngine engine() {
        return new FinancialPlanningEngine(recommendationEngine, properties);
    }

    private FinancialGoal goal(String target, String current, LocalDate targetDate) {
        return FinancialGoal.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).title("Laptop").goalType(GoalType.GADGET)
                .targetAmount(new BigDecimal(target)).currentAmount(new BigDecimal(current)).targetDate(targetDate)
                .status(GoalStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private ForecastContext context(String savedThisMonth, int health) {
        return ForecastFixtures.context(
                ForecastFixtures.insight(TODAY, List.of(), List.of(), null, 0), "0", savedThisMonth, health);
    }

    @Test
    void feasibleGoalYieldsAStraightforwardPlan() {
        // ₹120,000 over 8 months = ₹15,000/month; capacity ₹20,000 → feasible.
        var goal = goal("120000", "0", LocalDate.of(2027, 3, 31));
        GoalPlanResponse plan = engine().buildPlan(goal, context("20000", 70));

        assertThat(plan.getRequiredMonthlySaving()).isEqualByComparingTo("15000");
        assertThat(plan.isFeasible()).isTrue();
        assertThat(plan.getSuccessProbability()).isGreaterThan(0.8);
        assertThat(plan.getSummary()).contains("Save").contains("15,000");
    }

    @Test
    void impossibleGoalIsDetectedAndAdvisesExtension() {
        when(recommendationEngine.generate(any())).thenReturn(List.of());
        // ₹300,000 in 2 months = ₹150,000/month; capacity only ₹2,000 → impossible.
        var goal = goal("300000", "0", LocalDate.of(2026, 9, 30));
        GoalPlanResponse plan = engine().buildPlan(goal, context("2000", 30));

        assertThat(plan.isFeasible()).isFalse();
        assertThat(plan.getSuccessProbability()).isLessThan(0.3);
        assertThat(plan.getAlternativeTargetDate()).isNotNull();
        assertThat(plan.getSummary()).contains("unlikely to achieve");
        assertThat(plan.getRecommendedActions()).isNotEmpty();
    }

    @Test
    void alreadyReachedGoalIsComplete() {
        var goal = goal("50000", "50000", LocalDate.of(2027, 1, 31));
        GoalPlanResponse plan = engine().buildPlan(goal, context("1000", 60));

        assertThat(plan.getRequiredMonthlySaving()).isEqualByComparingTo("0");
        assertThat(plan.getSuccessProbability()).isEqualTo(1.0);
        assertThat(plan.isFeasible()).isTrue();
    }
}
