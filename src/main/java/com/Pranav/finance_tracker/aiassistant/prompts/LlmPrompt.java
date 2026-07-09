package com.Pranav.finance_tracker.aiassistant.prompts;

import com.Pranav.finance_tracker.aiassistant.dto.ConversationTurn;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * A fully-assembled, provider-agnostic prompt. It carries only pre-computed, structured facts
 * (never raw database objects), so any LLM provider can render a trustworthy answer.
 */
@Getter
@Builder
public class LlmPrompt {

    /** System instructions governing tone and the no-hallucination contract. */
    private final String systemPrompt;

    /** The user's question. */
    private final String userQuestion;

    /** Concise, labelled financial facts gathered from the selected tools. */
    private final String factContext;

    /** Recent conversation turns for lightweight continuity (may be empty). */
    private final List<ConversationTurn> recentHistory;

    /** Whether any tool returned usable data. */
    private final boolean hasFacts;
}
