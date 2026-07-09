package com.Pranav.finance_tracker.financialintelligence.scheduler;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialInsightSchedulerTest {

    @Mock private UserRepository userRepository;
    @Mock private FinancialInsightService insightService;

    @InjectMocks private FinancialInsightScheduler scheduler;

    @Test
    void processesEveryUser() {
        User a = TestFixtures.user();
        User b = TestFixtures.user();
        when(userRepository.findAll()).thenReturn(List.of(a, b));
        when(insightService.generateForUser(any())).thenReturn(2);

        scheduler.generateNightlyInsights();

        verify(insightService).generateForUser(a);
        verify(insightService).generateForUser(b);
    }

    @Test
    void continuesAfterAFailingUser() {
        User a = TestFixtures.user();
        User b = TestFixtures.user();
        when(userRepository.findAll()).thenReturn(List.of(a, b));
        when(insightService.generateForUser(a)).thenThrow(new RuntimeException("db down"));
        when(insightService.generateForUser(b)).thenReturn(1);

        scheduler.generateNightlyInsights();

        // Both users attempted despite the first throwing.
        verify(insightService, times(1)).generateForUser(a);
        verify(insightService, times(1)).generateForUser(b);
    }
}
