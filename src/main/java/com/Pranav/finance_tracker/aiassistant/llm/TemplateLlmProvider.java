package com.Pranav.finance_tracker.aiassistant.llm;

import com.Pranav.finance_tracker.aiassistant.prompts.LlmPrompt;
import org.springframework.stereotype.Component;

/**
 * Default, fully-offline {@link LlmProvider}.
 *
 * <p>It composes the answer <b>only</b> from the pre-computed facts in the prompt, which makes
 * hallucinated or invented numbers structurally impossible — a deterministic realisation of the
 * module's "never let the LLM guess" contract. It is the safe out-of-the-box provider; a
 * generative provider (OpenAI/Anthropic/…) can replace it via {@code ai.provider} while keeping the
 * exact same guarantee by being fed this same fact-only prompt.</p>
 */
@Component
public class TemplateLlmProvider implements LlmProvider {

    public static final String NAME = "template";

    /** The exact message the assistant must give when it lacks data. */
    public static final String NO_DATA_MESSAGE =
            "I don't currently have enough financial information to answer that accurately.";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public LlmResponse generate(LlmPrompt prompt) {
        String text;
        if (!prompt.isHasFacts() || prompt.getFactContext() == null || prompt.getFactContext().isBlank()) {
            text = NO_DATA_MESSAGE;
        } else {
            // The fact context is already a set of concise, user-facing sentences from the backend.
            text = "Here's what your financial data shows: " + prompt.getFactContext().trim();
        }
        return LlmResponse.builder().text(text).providerName(NAME).build();
    }
}
