package com.Pranav.finance_tracker.financialintelligence.healthscore.service;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScoreResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScoreResult;
import com.Pranav.finance_tracker.financialintelligence.healthscore.engine.HealthScoreEngine;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.FinancialHealthScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthBand;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import com.Pranav.finance_tracker.financialintelligence.healthscore.repository.FinancialHealthScoreRepository;
import com.Pranav.finance_tracker.financialintelligence.notification.InsightNotificationService;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthScoreServiceTest {

    @Mock private HealthScoreEngine engine;
    @Mock private InsightContextFactory contextFactory;
    @Mock private FinancialHealthScoreRepository repository;
    @Mock private InsightNotificationService notificationService;

    private HealthScoreService service;
    private User user;
    private InsightContext ctx;

    @BeforeEach
    void setUp() {
        service = new HealthScoreService(engine, contextFactory, repository, new HealthScoreProperties(), notificationService);
        user = TestFixtures.user();
        ctx = TestFixtures.riskContext().build();
    }

    private HealthScoreResult result(int overall, int budget, int savings, int debt, int spending) {
        List<ComponentScore> components = List.of(
                cs(HealthComponent.BUDGET_ADHERENCE, budget, 30),
                cs(HealthComponent.SAVINGS_BEHAVIOR, savings, 30),
                cs(HealthComponent.DEBT_MANAGEMENT, debt, 25),
                cs(HealthComponent.SPENDING_STABILITY, spending, 15));
        return HealthScoreResult.builder()
                .overallScore(overall).band(HealthBand.fromScore(overall)).components(components)
                .explanation("Your financial health score is " + overall + "/100.").build();
    }

    private ComponentScore cs(HealthComponent c, int score, int max) {
        return ComponentScore.builder().component(c).score(score).maxPoints(max).reason("r").build();
    }

    private FinancialHealthScore previous(int score, int budget, int savings, int debt, int spending) {
        return FinancialHealthScore.builder()
                .userId(user.getId()).score(score).band(HealthBand.fromScore(score))
                .budgetScore(budget).savingsScore(savings).debtScore(debt).spendingScore(spending)
                .explanation("prev").createdAt(LocalDateTime.now().minusDays(1)).build();
    }

    @Test
    void generatePersistsAndNotifiesOnSignificantDrop() {
        when(engine.evaluate(any())).thenReturn(result(60, 15, 15, 20, 10));
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(previous(80, 25, 30, 25, 0));
        when(repository.existsByUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(false);

        int score = service.generateForUser(user, ctx);

        assertThat(score).isEqualTo(60);
        verify(repository).save(any());
        verify(notificationService).notifyHealthScore(eq(user.getId()), contains("dropped 20 points"));
    }

    @Test
    void generateSkipsWhenAlreadyRecordedToday() {
        when(engine.evaluate(any())).thenReturn(result(60, 15, 15, 20, 10));
        when(repository.existsByUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(true);

        service.generateForUser(user, ctx);

        verify(repository, never()).save(any());
        verify(notificationService, never()).notifyHealthScore(any(), any());
    }

    @Test
    void currentScoreComputesLiveAndExplainsChange() {
        when(contextFactory.build(user)).thenReturn(ctx);
        when(engine.evaluate(any())).thenReturn(result(70, 21, 30, 19, 0));
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(previous(65, 21, 25, 19, 0));

        HealthScoreResponse response = service.getCurrentScore(user);

        assertThat(response.getScore()).isEqualTo(70);
        assertThat(response.getChangeSincePrevious()).isEqualTo(5);
        assertThat(response.getComponents()).hasSize(4);
        assertThat(response.getChangeExplanation()).contains("improved");
    }

    @Test
    void currentScoreHasNoChangeWhenNoPreviousSnapshot() {
        when(contextFactory.build(user)).thenReturn(ctx);
        when(engine.evaluate(any())).thenReturn(result(70, 21, 30, 19, 0));
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(null);

        HealthScoreResponse response = service.getCurrentScore(user);

        assertThat(response.getChangeSincePrevious()).isNull();
        assertThat(response.getChangeExplanation()).isNull();
    }
}
