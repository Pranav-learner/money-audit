package com.Pranav.finance_tracker.financialintelligence.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Creates in-app notifications for the Financial Intelligence Engine.
 *
 * <p>This service only <b>persists</b> a notification for later WebSocket / frontend
 * delivery. It deliberately does not send email — that is out of scope for this feature.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsightNotificationService {

    static final String TYPE_FINANCIAL_INSIGHT = "FINANCIAL_INSIGHT";
    private static final String TITLE = "Your financial analysis is ready";

    private final InAppNotificationRepository notificationRepository;

    /**
     * Persists a single notification telling the user that {@code newInsightCount}
     * fresh insights are available.
     *
     * @param userId          owner of the notification
     * @param newInsightCount number of newly generated insights (must be &gt; 0 to be meaningful)
     */
    public void notifyInsightsReady(UUID userId, int newInsightCount) {
        String body = buildBody(newInsightCount);

        InAppNotification notification = InAppNotification.builder()
                .userId(userId)
                .title(TITLE)
                .body(body)
                .type(TYPE_FINANCIAL_INSIGHT)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.debug("Persisted financial-insight notification for user {} ({} insights)", userId, newInsightCount);
    }

    private String buildBody(int count) {
        String noun = count == 1 ? "insight" : "insights";
        return String.format("We found %d new personalized %s.", count, noun);
    }
}
