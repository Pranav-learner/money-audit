package com.Pranav.finance_tracker.financialintelligence.forecast.service;

import com.Pranav.finance_tracker.exception.ResourceNotFoundException;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.ForecastFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.config.ForecastProperties;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalPlanResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalRequest;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.FinancialGoal;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalStatus;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalType;
import com.Pranav.finance_tracker.financialintelligence.forecast.planner.FinancialPlanningEngine;
import com.Pranav.finance_tracker.financialintelligence.notification.InsightNotificationService;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private com.Pranav.finance_tracker.financialintelligence.forecast.repository.FinancialGoalRepository goalRepository;
    @Mock private ForecastContextFactory contextFactory;
    @Mock private FinancialPlanningEngine planningEngine;
    @Mock private InsightNotificationService notificationService;

    private GoalService service;
    private User user;
    private InsightContext insight;

    @BeforeEach
    void setUp() {
        service = new GoalService(goalRepository, contextFactory, planningEngine, notificationService, new ForecastProperties());
        user = TestFixtures.user();
        insight = TestFixtures.riskContext().build();
    }

    private GoalPlanResponse plan(String required, LocalDate projected) {
        return GoalPlanResponse.builder()
                .requiredMonthlySaving(new BigDecimal(required))
                .projectedCompletionDate(projected)
                .successProbability(0.7)
                .feasible(true)
                .build();
    }

    private FinancialGoal goal(String target, String current, GoalStatus status) {
        return FinancialGoal.builder()
                .id(UUID.randomUUID()).userId(user.getId()).title("Laptop").goalType(GoalType.GADGET)
                .targetAmount(new BigDecimal(target)).currentAmount(new BigDecimal(current))
                .targetDate(LocalDate.now().plusMonths(8)).status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void createGoalStoresDerivedPlanningFields() {
        when(contextFactory.build(user)).thenReturn(ForecastFixtures.context(insight, "0", "5000", 60));
        when(planningEngine.buildPlan(any(), any())).thenReturn(plan("15000", LocalDate.now().plusMonths(8)));

        GoalRequest request = new GoalRequest();
        request.setTitle("Laptop");
        request.setGoalType(GoalType.GADGET);
        request.setTargetAmount(new BigDecimal("120000"));
        request.setTargetDate(LocalDate.now().plusMonths(8));

        var response = service.createGoal(user, request);

        assertThat(response.getMonthlyContributionRequired()).isEqualByComparingTo("15000");
        ArgumentCaptor<FinancialGoal> captor = ArgumentCaptor.forClass(FinancialGoal.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(captor.getValue().getCurrentAmount()).isEqualByComparingTo("0"); // defaulted
    }

    @Test
    void deleteThrowsWhenNotOwned() {
        UUID id = UUID.randomUUID();
        when(goalRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteGoal(user, id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void analyzeMarksReachedGoalCompletedAndNotifies() {
        FinancialGoal reached = goal("50000", "50000", GoalStatus.ACTIVE);
        when(goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), GoalStatus.ACTIVE))
                .thenReturn(List.of(reached));
        when(contextFactory.build(eq(user), any())).thenReturn(ForecastFixtures.context(insight, "0", "1000", 60));
        when(planningEngine.buildPlan(any(), any())).thenReturn(plan("0", LocalDate.now()));

        int analysed = service.analyzeGoals(user, insight);

        assertThat(analysed).isEqualTo(1);
        assertThat(reached.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        verify(notificationService).notifyGoalUpdate(eq(user.getId()), contains("Goal complete"));
    }
}
