package com.Pranav.finance_tracker.aiassistant.orchestrator;

import com.Pranav.finance_tracker.aiassistant.orchestrator.strategy.*;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntentRouterTest {

    private final IntentRouter router = new IntentRouter(List.of(
            new ExpenseIntentStrategy(), new BudgetIntentStrategy(), new SavingsIntentStrategy(),
            new FinancialHealthIntentStrategy(), new RiskIntentStrategy(), new RecommendationIntentStrategy(),
            new ForecastIntentStrategy(), new GoalIntentStrategy(), new SplitwiseIntentStrategy(),
            new GeneralFinanceIntentStrategy()));

    @Test
    void classifiesExpenseQuestion() {
        RoutingResult r = router.route("Where did my money go this month?");
        assertThat(r.getIntent()).isEqualTo(Intent.EXPENSE_SUMMARY);
        assertThat(r.getToolKeys()).contains(ToolKeys.EXPENSE);
        assertThat(r.getConfidence()).isGreaterThan(0.5);
    }

    @Test
    void classifiesBudgetQuestion() {
        assertThat(router.route("Which budget is closest to being exceeded?").getIntent())
                .isEqualTo(Intent.BUDGET_ANALYSIS);
    }

    @Test
    void classifiesRiskQuestion() {
        RoutingResult r = router.route("What is my biggest financial risk right now?");
        assertThat(r.getIntent()).isEqualTo(Intent.RISK_ANALYSIS);
        assertThat(r.getToolKeys()).containsExactly(ToolKeys.RISK);
    }

    @Test
    void classifiesGoalQuestion() {
        RoutingResult r = router.route("When can I buy a new laptop?");
        assertThat(r.getIntent()).isEqualTo(Intent.GOAL_PLANNING);
        assertThat(r.getToolKeys()).contains(ToolKeys.GOAL, ToolKeys.FORECAST);
    }

    @Test
    void classifiesSplitwiseQuestion() {
        assertThat(router.route("Who should I settle first?").getIntent()).isEqualTo(Intent.SPLITWISE);
    }

    @Test
    void unrecognisedFinanceQuestionFallsBackToGeneral() {
        RoutingResult r = router.route("Tell me about my finances overall");
        assertThat(r.getIntent()).isIn(Intent.GENERAL_FINANCE, Intent.EXPENSE_SUMMARY); // 'finances' → general
        assertThat(r.getToolKeys()).isNotEmpty();
    }

    @Test
    void blankMessageIsUnknown() {
        RoutingResult r = router.route("   ");
        assertThat(r.getIntent()).isEqualTo(Intent.UNKNOWN);
        assertThat(r.getToolKeys()).isEmpty();
    }

    @Test
    void completelyUnrelatedMessageStillRoutesToAFallback() {
        RoutingResult r = router.route("hello there");
        assertThat(r.getIntent()).isEqualTo(Intent.GENERAL_FINANCE);
    }
}
