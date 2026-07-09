package com.Pranav.finance_tracker.financialintelligence.recommendation.engine;

import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.RecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs every registered {@link RecommendationRule} against a context and collects the drafts.
 *
 * <p>Spring injects <b>all</b> {@code RecommendationRule} beans, so adding a rule (heuristic or a
 * future ML model) requires no change here. A failure in one rule is logged and isolated so the
 * remaining rules still run. Prioritisation, de-duplication and persistence happen downstream.</p>
 */
@Component
@Slf4j
public class RecommendationEngine {

    private final List<RecommendationRule> rules;

    public RecommendationEngine(List<RecommendationRule> rules) {
        this.rules = rules;
        log.info("RecommendationEngine initialised with {} rule(s): {}", rules.size(),
                rules.stream().map(RecommendationRule::ruleKey).toList());
    }

    /**
     * Evaluates all rules and returns every draft they produce.
     *
     * @param context preloaded, health-scored data for one user
     * @return all drafts, in rule-registration order (never {@code null})
     */
    public List<RecommendationDraft> generate(RecommendationContext context) {
        List<RecommendationDraft> drafts = new ArrayList<>();
        for (RecommendationRule rule : rules) {
            try {
                List<RecommendationDraft> produced = rule.evaluate(context);
                if (produced != null) {
                    drafts.addAll(produced);
                }
            } catch (Exception ex) {
                Object userId = context.getInsight() != null && context.getInsight().getUser() != null
                        ? context.getInsight().getUser().getId() : "unknown";
                log.error("Recommendation rule '{}' failed for user {}: {}", rule.ruleKey(), userId, ex.getMessage(), ex);
            }
        }
        return drafts;
    }
}
