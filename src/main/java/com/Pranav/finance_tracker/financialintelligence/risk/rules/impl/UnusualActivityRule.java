package com.Pranav.finance_tracker.financialintelligence.risk.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.dto.SpendingAnomaly;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.RiskRule;
import com.Pranav.finance_tracker.financialintelligence.risk.service.SpendingAnomalyDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Risk Rule 8 — Unusual Spending.
 *
 * <p>Delegates entirely to a {@link SpendingAnomalyDetector}, mapping each detected
 * {@link SpendingAnomaly} onto an insight draft. Severity and confidence come straight from the
 * detector, so the rule contains no detection logic of its own.</p>
 *
 * <p><b>Future ML compatibility:</b> the detector is the seam — replacing the shipped statistical
 * implementation with a machine-learning model requires only a new {@code SpendingAnomalyDetector}
 * bean; this rule, its {@link FinancialRiskType}, the REST API and the schema are all untouched.</p>
 */
@Component
@RequiredArgsConstructor
public class UnusualActivityRule implements RiskRule {

    private static final String RULE_KEY = "UNUSUAL_ACTIVITY";

    private final SpendingAnomalyDetector anomalyDetector;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public FinancialRiskType riskType() {
        return FinancialRiskType.UNUSUAL_ACTIVITY;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        List<SpendingAnomaly> anomalies = anomalyDetector.detect(context);
        if (anomalies == null || anomalies.isEmpty()) {
            return List.of();
        }

        List<InsightDraft> drafts = new ArrayList<>();
        for (SpendingAnomaly anomaly : anomalies) {
            drafts.add(InsightDraft.builder()
                    .ruleKey(RULE_KEY + ":" + anomaly.getSignature())
                    .title("Unusual spending detected")
                    .description(anomaly.getExplanation())
                    .insightType(InsightType.SPENDING_WARNING)
                    .severity(anomaly.getSuggestedSeverity())
                    .riskType(FinancialRiskType.UNUSUAL_ACTIVITY)
                    .category(anomaly.getCategory())
                    .actionSuggestion("If you don't recognise this activity, review the transactions and "
                            + "confirm they're expected.")
                    .confidence(anomaly.getConfidence())
                    .build());
        }
        return drafts;
    }
}
