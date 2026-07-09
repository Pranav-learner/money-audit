package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.financialintelligence.notification.InAppNotification;
import com.Pranav.finance_tracker.financialintelligence.notification.InAppNotificationRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Exposes the user's recent unread in-app notifications via the Notification System. */
@Component
@RequiredArgsConstructor
public class NotificationTool implements FinancialTool {

    private final InAppNotificationRepository notificationRepository;

    @Override
    public String key() {
        return ToolKeys.NOTIFICATION;
    }

    @Override
    public String moduleLabel() {
        return "Notifications";
    }

    @Override
    public ToolResult fetch(User user) {
        List<InAppNotification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
        if (unread == null || unread.isEmpty()) {
            return ToolResult.of(key(), moduleLabel(), "You have no unread notifications.");
        }
        String latest = unread.get(0).getBody();
        String text = String.format("You have %d unread notification(s). Most recent: \"%s\".", unread.size(), latest);
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
