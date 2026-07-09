package com.Pranav.finance_tracker.financialintelligence.recommendation.service;

import com.Pranav.finance_tracker.analytics.dto.MonthlySavingsResponse;
import com.Pranav.finance_tracker.analytics.dto.TotalSavingsResponse;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.exception.ResourceNotFoundException;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.notification.InsightNotificationService;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.engine.RecommendationEngine;
import com.Pranav.finance_tracker.financialintelligence.recommendation.engine.RecommendationPriorityEngine;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.FinancialRecommendation;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationStatus;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.mapper.RecommendationMapper;
import com.Pranav.finance_tracker.financialintelligence.recommendation.repository.FinancialRecommendationRepository;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private FinancialRecommendationRepository repository;
    @Mock private RecommendationEngine engine;
    @Mock private RecommendationPriorityEngine priorityEngine;
    @Mock private HealthScoreProvider healthScoreProvider;
    @Mock private AnalyticsService analyticsService;
    @Mock private InsightNotificationService notificationService;

    private RecommendationService service;
    private User user;
    private InsightContext context;

    @BeforeEach
    void setUp() {
        RecommendationMapper mapper = new RecommendationMapper();
        service = new RecommendationService(repository, /* contextFactory unused here */ null, engine,
                priorityEngine, healthScoreProvider, mapper, analyticsService, notificationService,
                new RecommendationProperties());
        user = TestFixtures.user();
        context = TestFixtures.riskContext().build();
    }

    private void stubContextLoads() {
        when(healthScoreProvider.scoreFor(any())).thenReturn(50);
        when(analyticsService.getTotalSavings(user))
                .thenReturn(TotalSavingsResponse.builder().totalSavings(BigDecimal.ZERO).build());
        when(analyticsService.getMonthlySavings(eq(user), anyInt(), anyInt()))
                .thenReturn(MonthlySavingsResponse.builder().totalSaved(BigDecimal.ZERO).build());
    }

    @Test
    void generatesPersistsAndNotifiesForHighPriority() {
        stubContextLoads();
        RecommendationDraft high = RecoFixtures.draft("A", RecommendationType.SAVING, Priority.HIGH, "2000", 0.8);
        RecommendationDraft low = RecoFixtures.draft("B", RecommendationType.HABIT, Priority.LOW, "300", 0.8);
        when(engine.generate(any())).thenReturn(List.of(high, low));
        when(priorityEngine.score(any(), any())).thenReturn(50.0);
        when(priorityEngine.prioritize(any(), any())).thenReturn(List.of(high, low));
        when(repository.existsByUserIdAndRuleKeyAndCreatedAtBetween(any(), any(), any(), any())).thenReturn(false);

        int created = service.generateForUser(user, context);

        assertThat(created).isEqualTo(2);
        verify(repository).saveAll(any());
        verify(notificationService).notifyRecommendations(eq(user.getId()), eq(2), any());
    }

    @Test
    void skipsRecommendationsAlreadyGeneratedTodayAndDoesNotNotifyWhenOnlyLowPriority() {
        stubContextLoads();
        RecommendationDraft high = RecoFixtures.draft("A", RecommendationType.SAVING, Priority.HIGH, "2000", 0.8);
        RecommendationDraft low = RecoFixtures.draft("B", RecommendationType.HABIT, Priority.LOW, "300", 0.8);
        when(engine.generate(any())).thenReturn(List.of(high, low));
        when(priorityEngine.score(any(), any())).thenReturn(50.0);
        when(priorityEngine.prioritize(any(), any())).thenReturn(List.of(high, low));
        // The HIGH one already exists today; only the LOW one is new.
        when(repository.existsByUserIdAndRuleKeyAndCreatedAtBetween(any(), eq("A"), any(), any())).thenReturn(true);
        when(repository.existsByUserIdAndRuleKeyAndCreatedAtBetween(any(), eq("B"), any(), any())).thenReturn(false);

        int created = service.generateForUser(user, context);

        assertThat(created).isEqualTo(1);
        verify(notificationService, never()).notifyRecommendations(any(), anyInt(), any());
    }

    @Test
    void filtersOutLowConfidenceDrafts() {
        stubContextLoads();
        RecommendationDraft weak = RecoFixtures.draft("A", RecommendationType.SAVING, Priority.HIGH, "2000", 0.2);
        when(engine.generate(any())).thenReturn(List.of(weak));
        when(priorityEngine.prioritize(any(), any())).thenReturn(List.of());

        int created = service.generateForUser(user, context);

        assertThat(created).isZero();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void completeSetsStatus() {
        UUID id = UUID.randomUUID();
        FinancialRecommendation entity = active(id);
        when(repository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.of(entity));

        service.complete(user, id);

        assertThat(entity.getStatus()).isEqualTo(RecommendationStatus.COMPLETED);
    }

    @Test
    void dismissThrowsWhenNotOwned() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dismiss(user, id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void summaryAggregatesPotentialSavings() {
        FinancialRecommendation a = active(UUID.randomUUID());
        a.setExpectedMonthlySaving(new BigDecimal("2000"));
        a.setPriority(Priority.HIGH);
        FinancialRecommendation b = active(UUID.randomUUID());
        b.setExpectedMonthlySaving(new BigDecimal("1000"));
        b.setPriority(Priority.MEDIUM);

        when(repository.findByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(user.getId()), eq(RecommendationStatus.ACTIVE), any())).thenReturn(new java.util.ArrayList<>(List.of(a, b)));
        when(repository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(a, b));
        when(repository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), RecommendationStatus.COMPLETED))
                .thenReturn(List.of());
        when(repository.countByUserIdAndStatus(user.getId(), RecommendationStatus.COMPLETED)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(user.getId(), RecommendationStatus.DISMISSED)).thenReturn(0L);

        var summary = service.getSummary(user);

        assertThat(summary.getPotentialMonthlySavings()).isEqualByComparingTo("3000");
        assertThat(summary.getPotentialAnnualSavings()).isEqualByComparingTo("36000");
        assertThat(summary.getActiveCount()).isEqualTo(2);
        assertThat(summary.getHighestPriority()).isNotNull();
    }

    private FinancialRecommendation active(UUID id) {
        return FinancialRecommendation.builder()
                .id(id).userId(user.getId()).ruleKey("K").title("t").description("d")
                .recommendationType(RecommendationType.SAVING).priority(Priority.MEDIUM)
                .expectedMonthlySaving(BigDecimal.ZERO).confidence(0.8).actionText("go")
                .status(RecommendationStatus.ACTIVE).createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30)).build();
    }
}
