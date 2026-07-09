package com.Pranav.finance_tracker.financialintelligence.risk.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.dto.RecurringCharge;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.RiskRule;
import com.Pranav.finance_tracker.financialintelligence.risk.service.RecurringChargeDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Risk Rule 7 — Recurring Payment Reminder.
 *
 * <p>Reuses the {@link RecurringChargeDetector} to find recurring bills (rent, EMI, insurance,
 * internet, …) and reminds the user when one is due within the next
 * {@link RiskThresholdProperties#getRecurringDueWindowDays()} days and has not yet been paid this
 * month. Complements the subscription rule (which looks backwards at cost) by looking forward at
 * upcoming obligations.</p>
 */
@Component
@RequiredArgsConstructor
public class RecurringPaymentRule implements RiskRule {

    private static final String RULE_KEY = "RECURRING_PAYMENT";

    private final RecurringChargeDetector recurringChargeDetector;
    private final RiskThresholdProperties thresholds;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public FinancialRiskType riskType() {
        return FinancialRiskType.RECURRING_PAYMENT;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        List<RecurringCharge> charges = recurringChargeDetector.detect(context);
        if (charges == null || charges.isEmpty()) {
            return List.of();
        }

        LocalDate today = context.getToday();
        YearMonth currentMonth = context.getCurrentMonth();

        List<InsightDraft> drafts = new ArrayList<>();
        for (RecurringCharge charge : charges) {
            Integer typicalDay = charge.getTypicalDayOfMonth();
            if (typicalDay == null) {
                continue;
            }
            // Already paid this month? Then nothing is due.
            if (charge.getLastSeen() != null && YearMonth.from(charge.getLastSeen()).equals(currentMonth)) {
                continue;
            }

            int dueDay = Math.min(typicalDay, currentMonth.lengthOfMonth());
            LocalDate dueDate = currentMonth.atDay(dueDay);
            long daysUntil = ChronoUnit.DAYS.between(today, dueDate);
            if (daysUntil < 0 || daysUntil > thresholds.getRecurringDueWindowDays()) {
                continue;
            }

            drafts.add(toDraft(charge, dueDay, daysUntil));
        }
        return drafts;
    }

    private InsightDraft toDraft(RecurringCharge charge, int dueDay, long daysUntil) {
        String when = daysUntil == 0 ? "today"
                : "in " + daysUntil + " day" + (daysUntil == 1 ? "" : "s");
        String description = String.format(
                "Your recurring '%s' payment (~%s) is usually due around the %s — %s.",
                charge.getLabel(), MoneyFormatter.rupees(charge.getTypicalAmount()), ordinal(dueDay), when);

        return InsightDraft.builder()
                .ruleKey(RULE_KEY + ":" + charge.getLabel())
                .title("Upcoming payment: " + charge.getLabel())
                .description(description)
                .insightType(InsightType.INFORMATION)
                .severity(Severity.MEDIUM)
                .riskType(FinancialRiskType.RECURRING_PAYMENT)
                .category(charge.getCategory())
                .actionSuggestion("Make sure you have " + MoneyFormatter.rupees(charge.getTypicalAmount())
                        + " set aside for '" + charge.getLabel() + "'.")
                .confidence(0.7)
                .build();
    }

    private String ordinal(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }
}
