package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.FinancialGoalResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.service.GoalService;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/** Exposes the user's active goals and their progress via the Goal Planning Engine. */
@Component
@RequiredArgsConstructor
public class GoalTool implements FinancialTool {

    private final GoalService goalService;

    @Override
    public String key() {
        return ToolKeys.GOAL;
    }

    @Override
    public String moduleLabel() {
        return "Goals";
    }

    @Override
    public ToolResult fetch(User user) {
        List<FinancialGoalResponse> goals = goalService.getActiveGoals(user);
        if (goals == null || goals.isEmpty()) {
            return ToolResult.of(key(), moduleLabel(),
                    "You have no active goals yet. You can create one (e.g. a laptop or emergency fund) to get a savings plan.");
        }

        String detail = goals.stream().limit(3).map(g -> String.format(
                        "%s: %d%% funded, needs about %s/month%s",
                        g.getTitle(), g.getProgressPercent(),
                        MoneyFormatter.rupees(g.getMonthlyContributionRequired()),
                        g.getProjectedCompletionDate() == null ? "" : " (projected completion " + g.getProjectedCompletionDate() + ")"))
                .collect(Collectors.joining("; "));
        return ToolResult.of(key(), moduleLabel(), "Your active goals — " + detail + ".");
    }
}
