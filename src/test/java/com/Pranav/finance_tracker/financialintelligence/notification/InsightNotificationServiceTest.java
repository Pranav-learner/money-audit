package com.Pranav.finance_tracker.financialintelligence.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InsightNotificationServiceTest {

    @Mock private InAppNotificationRepository notificationRepository;
    @InjectMocks private InsightNotificationService service;

    @Test
    void risksDetectedNotificationHasRiskTitleAndCountBody() {
        UUID userId = UUID.randomUUID();

        service.notifyRisksDetected(userId, 3);

        ArgumentCaptor<InAppNotification> captor = ArgumentCaptor.forClass(InAppNotification.class);
        verify(notificationRepository).save(captor.capture());

        InAppNotification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTitle()).isEqualTo("Financial Risk Detected");
        assertThat(saved.getBody()).isEqualTo("We found 3 issues that require your attention.");
        assertThat(saved.getType()).isEqualTo(InsightNotificationService.TYPE_FINANCIAL_RISK);
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void singleRiskUsesSingularNoun() {
        service.notifyRisksDetected(UUID.randomUUID(), 1);

        ArgumentCaptor<InAppNotification> captor = ArgumentCaptor.forClass(InAppNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getBody()).isEqualTo("We found 1 issue that require your attention.");
    }

    @Test
    void insightsReadyNotificationUsesGenericTitle() {
        UUID userId = UUID.randomUUID();

        service.notifyInsightsReady(userId, 2);

        ArgumentCaptor<InAppNotification> captor = ArgumentCaptor.forClass(InAppNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(InsightNotificationService.TYPE_FINANCIAL_INSIGHT);
    }
}
