package com.Pranav.finance_tracker.aiassistant.prompts;

import com.Pranav.finance_tracker.aiassistant.dto.ConversationTurn;
import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds a provider-agnostic {@link LlmPrompt} from the user's question, the tool results and recent
 * conversation. It <b>never</b> passes raw database objects to the LLM — only the concise, labelled
 * fact summaries the tools produced, together with a system prompt that forbids inventing numbers.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are the AI Financial Assistant for the Money Audit platform. You are the voice of the \
            platform's financial engines, not a calculator. Rules:
            - Use ONLY the facts provided below. Never invent, estimate or guess numbers.
            - Be professional, concise, personalized and actionable.
            - Explain briefly why, using the provided facts.
            - If the facts are insufficient, say you don't have enough information.
            """;

    /**
     * @param question    the user's message
     * @param intent      the detected intent (for light framing)
     * @param toolResults results keyed by tool key
     * @param history     recent conversation turns
     */
    public LlmPrompt build(String question, Intent intent, Map<String, ToolResult> toolResults,
                           List<ConversationTurn> history) {
        String facts = toolResults.values().stream()
                .filter(ToolResult::isAvailable)
                .map(r -> "- [" + r.getModuleLabel() + "] " + r.getSummary())
                .collect(Collectors.joining("\n"));

        boolean hasFacts = !facts.isBlank();

        return LlmPrompt.builder()
                .systemPrompt(SYSTEM_PROMPT)
                .userQuestion(question)
                .factContext(flatten(toolResults))
                .recentHistory(history == null ? List.of() : history)
                .hasFacts(hasFacts)
                .build();
    }

    /** A single flowing string of the available facts, for providers that want plain text. */
    private String flatten(Map<String, ToolResult> toolResults) {
        return toolResults.values().stream()
                .filter(ToolResult::isAvailable)
                .map(ToolResult::getSummary)
                .collect(Collectors.joining(" "));
    }
}
