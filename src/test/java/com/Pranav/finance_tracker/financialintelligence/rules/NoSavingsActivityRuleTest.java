package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.rules.impl.NoSavingsActivityRule;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoSavingsActivityRuleTest {

    private final NoSavingsActivityRule rule = new NoSavingsActivityRule();

    private InsightContext context(User user, LocalDate lastSavingDate) {
        LocalDate today = LocalDate.now();
        return InsightContext.builder()
                .user(user)
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(List.of())
                .budgetUsages(List.of())
                .lastSavingDate(lastSavingDate)
                .build();
    }

    @Test
    void remindsWhenLastSavingIsOlderThanThreshold() {
        LocalDate lastSaving = LocalDate.now().minusDays(40);

        List<InsightDraft> drafts = rule.evaluate(context(TestFixtures.user(), lastSaving));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getInsightType()).isEqualTo(InsightType.INFORMATION);
        assertThat(drafts.get(0).getDescription()).contains("40 days");
    }

    @Test
    void noReminderWhenRecentSavingExists() {
        LocalDate lastSaving = LocalDate.now().minusDays(10);
        assertThat(rule.evaluate(context(TestFixtures.user(), lastSaving))).isEmpty();
    }

    @Test
    void noReminderForBrandNewUserWhoNeverSaved() {
        User newUser = User.builder()
                .id(java.util.UUID.randomUUID())
                .name("New")
                .email("new@example.com")
                .password("x")
                .createdAt(LocalDateTime.now().minusDays(10)) // account younger than threshold
                .build();

        assertThat(rule.evaluate(context(newUser, null))).isEmpty();
    }

    @Test
    void remindsEstablishedUserWhoNeverSaved() {
        User oldUser = User.builder()
                .id(java.util.UUID.randomUUID())
                .name("Old")
                .email("old@example.com")
                .password("x")
                .createdAt(LocalDateTime.now().minusDays(200))
                .build();

        List<InsightDraft> drafts = rule.evaluate(context(oldUser, null));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getDescription()).contains("haven't recorded any savings yet");
    }
}
