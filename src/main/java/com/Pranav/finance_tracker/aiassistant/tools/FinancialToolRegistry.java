package com.Pranav.finance_tracker.aiassistant.tools;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Indexes all {@link FinancialTool} beans by their key so the orchestrator can resolve the tools an
 * intent requests. New tools register automatically by being Spring beans (Open/Closed).
 */
@Component
public class FinancialToolRegistry {

    private final Map<String, FinancialTool> toolsByKey;

    public FinancialToolRegistry(List<FinancialTool> tools) {
        this.toolsByKey = tools.stream().collect(Collectors.toMap(FinancialTool::key, Function.identity()));
    }

    public Optional<FinancialTool> get(String key) {
        return Optional.ofNullable(toolsByKey.get(key));
    }
}
