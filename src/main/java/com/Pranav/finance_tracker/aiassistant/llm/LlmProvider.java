package com.Pranav.finance_tracker.aiassistant.llm;

import com.Pranav.finance_tracker.aiassistant.prompts.LlmPrompt;

/**
 * Adapter over an LLM backend.
 *
 * <p>The orchestrator depends only on this interface, so providers are swappable purely through
 * configuration ({@code ai.provider}). The shipped {@link TemplateLlmProvider} is fully offline and
 * deterministic; adding {@code OpenAiLlmProvider}, {@code AnthropicLlmProvider},
 * {@code AzureLlmProvider}, {@code GoogleLlmProvider} or a {@code LocalLlmProvider} means adding a
 * new bean with a distinct {@link #name()} — no orchestration or business code changes (Adapter
 * pattern, Open/Closed).</p>
 */
public interface LlmProvider {

    /** Unique provider key matched against {@code ai.provider}. */
    String name();

    /** Generates a natural-language answer from the structured prompt. Must never invent numbers. */
    LlmResponse generate(LlmPrompt prompt);
}
