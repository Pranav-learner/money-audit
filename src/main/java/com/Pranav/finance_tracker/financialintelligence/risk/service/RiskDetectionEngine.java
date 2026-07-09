package com.Pranav.finance_tracker.financialintelligence.risk.service;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.RiskRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes the Financial Risk Detection phase (Module 2).
 *
 * <p>Spring injects <b>all</b> {@link RiskRule} beans, so a new risk rule — heuristic or a future
 * ML model — is discovered automatically with no change here (Open/Closed Principle). Each rule
 * runs against the same preloaded {@link InsightContext} used by the spending-intelligence phase,
 * so risk detection adds <b>no extra per-user database scans</b>.</p>
 *
 * <p>The engine's single responsibility is <i>evaluating risk rules and collecting their drafts</i>.
 * Persistence and notification are intentionally left to the existing
 * {@code FinancialInsightService} so no storage/notification logic is duplicated — the drafts flow
 * into the very same dedup → persist → notify pipeline as Module 1's insights.</p>
 */
@Component
@Slf4j
public class RiskDetectionEngine {

    private final List<RiskRule> riskRules;

    public RiskDetectionEngine(List<RiskRule> riskRules) {
        this.riskRules = riskRules;
        log.info("RiskDetectionEngine initialised with {} risk rule(s): {}", riskRules.size(),
                riskRules.stream().map(RiskRule::ruleKey).toList());
    }

    /**
     * Evaluates every risk rule against the context and returns all drafts produced.
     * A failure in one rule is logged and isolated so the remaining rules still run.
     *
     * @param context preloaded financial data for one user
     * @return all risk drafts (never {@code null})
     */
    public List<InsightDraft> run(InsightContext context) {
        Object userId = context.getUser() != null ? context.getUser().getId() : "unknown";
        log.debug("Risk detection started for user {} with {} rule(s)", userId, riskRules.size());

        List<InsightDraft> drafts = new ArrayList<>();
        for (RiskRule rule : riskRules) {
            try {
                List<InsightDraft> produced = rule.evaluate(context);
                if (produced != null) {
                    drafts.addAll(produced);
                }
            } catch (Exception ex) {
                log.error("Risk rule '{}' failed for user {}: {}", rule.ruleKey(), userId, ex.getMessage(), ex);
            }
        }

        log.debug("Risk detection produced {} risk(s) for user {}", drafts.size(), userId);
        return drafts;
    }
}
