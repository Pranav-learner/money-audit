package com.Pranav.finance_tracker.aiassistant.tools;

import com.Pranav.finance_tracker.user.entity.User;

/**
 * A thin wrapper around an existing backend service that exposes one slice of the user's financial
 * data to the AI assistant.
 *
 * <p>Tools <b>never access repositories directly</b> and <b>never contain business logic</b> — they
 * delegate to the same services the REST controllers use, so the backend stays the single source of
 * truth. Each tool is a Spring bean discovered by the {@link FinancialToolRegistry}.</p>
 */
public interface FinancialTool {

    /** Stable key (see {@link ToolKeys}) used for selection and logging. */
    String key();

    /** The business module label surfaced to the user as a "referenced module". */
    String moduleLabel();

    /**
     * Fetches this tool's structured data for the given user. Implementations must not throw for
     * "no data" — they return {@link ToolResult#unavailable} instead.
     */
    ToolResult fetch(User user);
}
