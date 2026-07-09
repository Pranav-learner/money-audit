package com.Pranav.finance_tracker.financialintelligence.risk.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.RiskRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Risk Rule 3 — Debt Risk.
 *
 * <p>Analyses the user's outstanding balances across Splitwise-style group splits and direct
 * friend settlements (surfaced by {@code AnalyticsService.getBalanceOverview} and aggregated into
 * the {@link InsightContext}). A warning is raised when the total owed crosses the configured
 * {@link RiskThresholdProperties#getDebtThreshold()} or when the number of unsettled settlements
 * reaches {@link RiskThresholdProperties#getOverdueSettlementCount()}.</p>
 */
@Component
@RequiredArgsConstructor
public class DebtRiskRule implements RiskRule {

    private static final String RULE_KEY = "DEBT_RISK";

    private final RiskThresholdProperties thresholds;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public FinancialRiskType riskType() {
        return FinancialRiskType.DEBT_RISK;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        BigDecimal owed = context.getTotalOwed();
        if (owed == null || owed.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        int settlements = context.getOwedSettlementCount();

        boolean overAmount = owed.compareTo(thresholds.getDebtThreshold()) >= 0;
        boolean tooManySettlements = settlements >= thresholds.getOverdueSettlementCount();
        if (!overAmount && !tooManySettlements) {
            return List.of();
        }

        boolean severe = owed.compareTo(thresholds.getDebtThreshold().multiply(BigDecimal.valueOf(2))) >= 0
                || settlements >= thresholds.getOverdueSettlementCount() * 2;
        Severity severity = severe ? Severity.HIGH : Severity.MEDIUM;

        String settlementClause = settlements > 0
                ? String.format(" across %d settlement%s", settlements, settlements == 1 ? "" : "s")
                : "";
        String description = String.format("You currently owe %s%s.", MoneyFormatter.rupees(owed), settlementClause);

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Outstanding debt is building up")
                .description(description)
                .insightType(InsightType.SPENDING_WARNING)
                .severity(severity)
                .riskType(FinancialRiskType.DEBT_RISK)
                .category(null)
                .actionSuggestion("Settle your highest outstanding balances first to reduce what you owe.")
                .confidence(0.9)
                .build();

        return List.of(draft);
    }
}
