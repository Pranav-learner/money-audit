package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.RiskRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs every registered {@link InsightRule} against a context and collects the resulting drafts.
 *
 * <p>Spring injects <b>all</b> {@code InsightRule} beans, so adding a rule requires no change here.
 * A failure in one rule is logged and isolated — it never prevents the other rules from running.</p>
 *
 * <p>{@link RiskRule} beans are deliberately excluded here: although a risk rule <i>is</i> an
 * {@code InsightRule}, it belongs to the Risk Detection phase and is run by the
 * {@link com.Pranav.finance_tracker.financialintelligence.risk.service.RiskDetectionEngine}. This
 * keeps the two phases distinct while both share the same rule contract and downstream pipeline.</p>
 */
@Component
@Slf4j
public class InsightEngine {

    private final List<InsightRule> rules;

    public InsightEngine(List<InsightRule> rules) {
        // Spending-intelligence rules only; risk rules are executed by the RiskDetectionEngine.
        this.rules = rules.stream()
                .filter(rule -> !(rule instanceof RiskRule))
                .toList();
        log.info("InsightEngine initialised with {} spending rule(s): {}", this.rules.size(),
                this.rules.stream().map(InsightRule::ruleKey).toList());
    }

    /**
     * Evaluates all rules and returns every draft they produce.
     *
     * @param context preloaded data for one user
     * @return all drafts, in rule-registration order (never {@code null})
     */
    public List<InsightDraft> run(InsightContext context) {
        List<InsightDraft> drafts = new ArrayList<>();
        for (InsightRule rule : rules) {
            try {
                List<InsightDraft> produced = rule.evaluate(context);
                if (produced != null) {
                    drafts.addAll(produced);
                }
            } catch (Exception ex) {
                log.error("Rule '{}' failed for user {}: {}",
                        rule.ruleKey(),
                        context.getUser() != null ? context.getUser().getId() : "unknown",
                        ex.getMessage(), ex);
            }
        }
        return drafts;
    }
}
