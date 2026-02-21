package com.Pranav.finance_tracker.email.scheduler;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.analytics.dto.FinancialSummaryResponse;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.email.repository.EmailLogRepository;
import com.Pranav.finance_tracker.email.service.EmailService;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailScheduler {

    private final EmailService emailService;
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final EmailLogRepository emailLogRepository;

    // Weekly Summary: Every Sunday at 9 PM
    @Scheduled(cron = "0 0 21 * * SUN")
    public void sendWeeklySummary() {
        log.info("Starting Weekly Summary Email Job");
        List<User> users = userRepository.findAll();
        LocalDate now = LocalDate.now();
        
        for (User user : users) {
            FinancialSummaryResponse summary = analyticsService.getMonthlySummary(user, now.getMonthValue(), now.getYear());
            String subject = "Your Weekly Financial Summary - " + now.getMonth() + " " + now.getYear();
            String body = String.format(
                    "Hello %s,\n\nHere is your financial summary for this month so far:\n" +
                    "- Total Saved: %.2f\n" +
                    "- Total Spent: %.2f\n" +
                    "- Net Savings: %.2f\n" +
                    "- Top Spending Category: %s\n\n" +
                    "Keep tracking your expenses with Finance Tracker!",
                    user.getName(), summary.getTotalIncomeSaved(), summary.getTotalSpent(),
                    summary.getNetSavings(), summary.getTopCategory()
            );
            emailService.sendEmail(user, subject, body);
        }
    }

    // Over Budget Alerts: Every day at 8 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void sendOverBudgetAlerts() {
        log.info("Starting Over Budget Alert Job");
        List<User> users = userRepository.findAll();
        LocalDate now = LocalDate.now();

        for (User user : users) {
            List<BudgetUsageResponse> usage = analyticsService.getBudgetUsage(user, now.getMonthValue(), now.getYear());
            for (BudgetUsageResponse budget : usage) {
                if ("OVER_BUDGET".equals(budget.getStatus())) {
                    String subject = "Budget Alert: Over Limit in " + budget.getCategory();
                    String body = String.format(
                            "Hello %s,\n\nYou have exceeded your budget for %s.\n" +
                            "- Budget Limit: %.2f\n" +
                            "- Amount Spent: %.2f\n" +
                            "- Percentage Used: %d%%\n\n" +
                            "Please review your expenses.",
                            user.getName(), budget.getCategory(), budget.getBudget(),
                            budget.getSpent(), budget.getPercentageUsed()
                    );
                    emailService.sendEmail(user, subject, body);
                }
            }
        }
    }

    // Cleanup Email Logs: Every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldLogs() {
        log.info("Starting Email Log Cleanup Job");
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
        emailLogRepository.deleteBySentAtBefore(fourteenDaysAgo);
        log.info("Deleted email logs older than 14 days");
    }
}
