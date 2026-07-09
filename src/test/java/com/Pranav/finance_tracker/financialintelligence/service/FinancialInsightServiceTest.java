package com.Pranav.finance_tracker.financialintelligence.service;

import com.Pranav.finance_tracker.exception.ResourceNotFoundException;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.FinancialInsight;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.mapper.InsightMapper;
import com.Pranav.finance_tracker.financialintelligence.notification.InsightNotificationService;
import com.Pranav.finance_tracker.financialintelligence.repository.FinancialInsightRepository;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.service.RiskDetectionEngine;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightEngine;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
class FinancialInsightServiceTest {

    @Mock private FinancialInsightRepository insightRepository;
    @Mock private InsightContextFactory contextFactory;
    @Mock private InsightEngine insightEngine;
    @Mock private RiskDetectionEngine riskDetectionEngine;
    @Mock private InsightNotificationService notificationService;
    @Spy private InsightMapper insightMapper = new InsightMapper();

    @InjectMocks private FinancialInsightService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestFixtures.user();
    }

    private InsightDraft draft(String key) {
        return InsightDraft.builder()
                .ruleKey(key)
                .title(key)
                .description("desc")
                .insightType(InsightType.INFORMATION)
                .severity(Severity.LOW)
                .confidence(0.8)
                .build();
    }

    @Test
    void generatesPersistsAndNotifiesForNewInsights() {
        when(insightEngine.run(any())).thenReturn(List.of(draft("A"), draft("B")));
        when(riskDetectionEngine.run(any())).thenReturn(List.of());
        when(insightRepository.existsByUserIdAndRuleKeyAndCreatedAtBetween(any(), any(), any(), any()))
                .thenReturn(false);

        int created = service.generateForUser(user);

        assertThat(created).isEqualTo(2);

        ArgumentCaptor<List<FinancialInsight>> captor = ArgumentCaptor.forClass(List.class);
        verify(insightRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        verify(notificationService).notifyInsightsReady(eq(user.getId()), eq(2));
    }

    @Test
    void raisesRiskNotificationWhenHighSeverityRiskIsGenerated() {
        InsightDraft highRisk = InsightDraft.builder()
                .ruleKey("BUDGET_RISK")
                .title("Budget at risk")
                .description("desc")
                .insightType(InsightType.BUDGET_ALERT)
                .severity(Severity.HIGH)
                .riskType(FinancialRiskType.BUDGET_RISK)
                .confidence(0.95)
                .build();
        when(insightEngine.run(any())).thenReturn(List.of());
        when(riskDetectionEngine.run(any())).thenReturn(List.of(highRisk));
        when(insightRepository.existsByUserIdAndRuleKeyAndCreatedAtBetween(any(), any(), any(), any()))
                .thenReturn(false);

        int created = service.generateForUser(user);

        assertThat(created).isEqualTo(1);
        verify(notificationService).notifyInsightsReady(eq(user.getId()), eq(1));
        verify(notificationService).notifyRisksDetected(eq(user.getId()), eq(1));
    }

    @Test
    void skipsDraftsAlreadyGeneratedToday() {
        when(insightEngine.run(any())).thenReturn(List.of(draft("A"), draft("B")));
        when(riskDetectionEngine.run(any())).thenReturn(List.of());
        // "A" already exists today, "B" is new.
        when(insightRepository.existsByUserIdAndRuleKeyAndCreatedAtBetween(any(), eq("A"), any(), any()))
                .thenReturn(true);
        when(insightRepository.existsByUserIdAndRuleKeyAndCreatedAtBetween(any(), eq("B"), any(), any()))
                .thenReturn(false);

        int created = service.generateForUser(user);

        assertThat(created).isEqualTo(1);
        verify(notificationService).notifyInsightsReady(eq(user.getId()), eq(1));
    }

    @Test
    void noNotificationWhenNothingNewIsGenerated() {
        when(insightEngine.run(any())).thenReturn(List.of());
        when(riskDetectionEngine.run(any())).thenReturn(List.of());

        int created = service.generateForUser(user);

        assertThat(created).isZero();
        verify(insightRepository, never()).saveAll(any());
        verify(notificationService, never()).notifyInsightsReady(any(), anyInt());
    }

    @Test
    void markAsReadThrowsWhenInsightNotOwned() {
        UUID id = UUID.randomUUID();
        when(insightRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAsReadSetsViewedFlag() {
        UUID id = UUID.randomUUID();
        FinancialInsight insight = FinancialInsight.builder()
                .id(id).userId(user.getId()).ruleKey("A").title("t").description("d")
                .insightType(InsightType.INFORMATION).severity(Severity.LOW)
                .confidence(0.5).createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(1))
                .viewed(false).dismissed(false).build();
        when(insightRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.of(insight));

        service.markAsRead(user, id);

        assertThat(insight.isViewed()).isTrue();
    }
}
