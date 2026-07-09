package com.Pranav.finance_tracker.aiassistant.prompts;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    @Test
    void includesOnlyAvailableFactsAndFlagsHasFacts() {
        Map<String, ToolResult> results = Map.of(
                "budget", ToolResult.of("budget", "Budgets", "Food is at 93%."),
                "risk", ToolResult.unavailable("risk", "Risk Detection"));

        LlmPrompt prompt = builder.build("How are my budgets?", Intent.BUDGET_ANALYSIS, results, List.of());

        assertThat(prompt.isHasFacts()).isTrue();
        assertThat(prompt.getFactContext()).contains("Food is at 93%");
        assertThat(prompt.getSystemPrompt()).contains("Never invent");
        assertThat(prompt.getUserQuestion()).isEqualTo("How are my budgets?");
    }

    @Test
    void hasFactsIsFalseWhenNothingAvailable() {
        Map<String, ToolResult> results = Map.of("risk", ToolResult.unavailable("risk", "Risk Detection"));

        LlmPrompt prompt = builder.build("What's my risk?", Intent.RISK_ANALYSIS, results, List.of());

        assertThat(prompt.isHasFacts()).isFalse();
        assertThat(prompt.getFactContext()).isBlank();
    }
}
