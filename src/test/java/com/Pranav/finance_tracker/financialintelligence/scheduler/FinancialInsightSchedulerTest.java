package com.Pranav.finance_tracker.financialintelligence.scheduler;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationService;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.financialintelligence.service.FinancialInsightService;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialInsightSchedulerTest {

    @Mock private UserRepository userRepository;
    @Mock private InsightContextFactory contextFactory;
    @Mock private FinancialInsightService insightService;
    @Mock private RecommendationService recommendationService;

    @InjectMocks private FinancialInsightScheduler scheduler;

    @Test
    void processesEveryUserThroughBothEngines() {
        User a = TestFixtures.user();
        User b = TestFixtures.user();
        when(userRepository.findAll()).thenReturn(List.of(a, b));
        when(contextFactory.build(any())).thenReturn(InsightContext.builder().build());
        when(insightService.generateForUser(any(), any())).thenReturn(2);
        when(recommendationService.generateForUser(any(), any())).thenReturn(1);

        scheduler.generateNightlyInsights();

        verify(insightService).generateForUser(eq(a), any());
        verify(insightService).generateForUser(eq(b), any());
        verify(recommendationService).generateForUser(eq(a), any());
        verify(recommendationService).generateForUser(eq(b), any());
    }

    @Test
    void continuesAfterAFailingUser() {
        User a = TestFixtures.user();
        User b = TestFixtures.user();
        when(userRepository.findAll()).thenReturn(List.of(a, b));
        when(contextFactory.build(any())).thenReturn(InsightContext.builder().build());
        when(insightService.generateForUser(eq(a), any())).thenThrow(new RuntimeException("db down"));
        when(insightService.generateForUser(eq(b), any())).thenReturn(1);
        when(recommendationService.generateForUser(eq(b), any())).thenReturn(1);

        scheduler.generateNightlyInsights();

        // Both users attempted despite the first throwing.
        verify(insightService, times(1)).generateForUser(eq(a), any());
        verify(insightService, times(1)).generateForUser(eq(b), any());
        verify(recommendationService, times(1)).generateForUser(eq(b), any());
    }
}
