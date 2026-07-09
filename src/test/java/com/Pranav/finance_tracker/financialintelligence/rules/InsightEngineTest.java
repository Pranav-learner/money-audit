package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsightEngineTest {

    private InsightContext emptyContext() {
        LocalDate today = LocalDate.now();
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(List.of())
                .budgetUsages(List.of())
                .build();
    }

    private InsightDraft draft(String key) {
        return InsightDraft.builder()
                .ruleKey(key)
                .title(key)
                .description("desc")
                .insightType(InsightType.INFORMATION)
                .severity(Severity.LOW)
                .confidence(0.5)
                .build();
    }

    @Test
    void aggregatesDraftsFromAllRules() {
        InsightRule ruleA = stub("A", List.of(draft("A1")));
        InsightRule ruleB = stub("B", List.of(draft("B1"), draft("B2")));

        InsightEngine engine = new InsightEngine(List.of(ruleA, ruleB));
        List<InsightDraft> drafts = engine.run(emptyContext());

        assertThat(drafts).extracting(InsightDraft::getRuleKey).containsExactly("A1", "B1", "B2");
    }

    @Test
    void oneFailingRuleDoesNotAbortTheOthers() {
        InsightRule failing = stubThrowing("BOOM");
        InsightRule healthy = stub("OK", List.of(draft("OK1")));

        InsightEngine engine = new InsightEngine(List.of(failing, healthy));
        List<InsightDraft> drafts = engine.run(emptyContext());

        assertThat(drafts).extracting(InsightDraft::getRuleKey).containsExactly("OK1");
    }

    private InsightRule stub(String key, List<InsightDraft> result) {
        return new InsightRule() {
            @Override
            public String ruleKey() {
                return key;
            }

            @Override
            public List<InsightDraft> evaluate(InsightContext context) {
                return result;
            }
        };
    }

    private InsightRule stubThrowing(String key) {
        return new InsightRule() {
            @Override
            public String ruleKey() {
                return key;
            }

            @Override
            public List<InsightDraft> evaluate(InsightContext context) {
                throw new IllegalStateException("rule blew up");
            }
        };
    }
}
