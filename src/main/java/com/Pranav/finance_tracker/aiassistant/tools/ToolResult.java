package com.Pranav.finance_tracker.aiassistant.tools;

import lombok.Builder;
import lombok.Getter;

/**
 * The structured outcome of a {@link FinancialTool} call.
 *
 * <p>Deliberately carries a concise, already-rendered {@code summary} string (never raw database
 * objects), so the prompt builder can hand the LLM trustworthy, pre-computed facts. When a tool has
 * no data it returns {@link #unavailable}, letting the assistant honestly say it lacks information
 * instead of guessing.</p>
 */
@Getter
@Builder
public class ToolResult {

    private final String toolKey;

    /** The business module this data came from (e.g. {@code Analytics}), for the "referenced modules" list. */
    private final String moduleLabel;

    /** Whether usable data was found. */
    private final boolean available;

    /** Concise, human-readable facts for the prompt — the single source of truth for the answer. */
    private final String summary;

    public static ToolResult unavailable(String toolKey, String moduleLabel) {
        return ToolResult.builder()
                .toolKey(toolKey).moduleLabel(moduleLabel).available(false)
                .summary("No data available.").build();
    }

    public static ToolResult of(String toolKey, String moduleLabel, String summary) {
        return ToolResult.builder()
                .toolKey(toolKey).moduleLabel(moduleLabel).available(true).summary(summary).build();
    }
}
