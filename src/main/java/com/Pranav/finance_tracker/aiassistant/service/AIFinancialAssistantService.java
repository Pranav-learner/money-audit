package com.Pranav.finance_tracker.aiassistant.service;

import com.Pranav.finance_tracker.aiassistant.config.AiAssistantProperties;
import com.Pranav.finance_tracker.aiassistant.dto.ChatResponse;
import com.Pranav.finance_tracker.aiassistant.dto.ConversationHistoryResponse;
import com.Pranav.finance_tracker.aiassistant.dto.ConversationTurn;
import com.Pranav.finance_tracker.aiassistant.llm.LlmProviderResolver;
import com.Pranav.finance_tracker.aiassistant.llm.LlmResponse;
import com.Pranav.finance_tracker.aiassistant.orchestrator.IntentRouter;
import com.Pranav.finance_tracker.aiassistant.orchestrator.RoutingResult;
import com.Pranav.finance_tracker.aiassistant.orchestrator.ToolOrchestrator;
import com.Pranav.finance_tracker.aiassistant.prompts.LlmPrompt;
import com.Pranav.finance_tracker.aiassistant.prompts.PromptBuilder;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * The AI orchestrator: understand → route → collect data → build prompt → call LLM → respond.
 *
 * <p>It performs <b>no financial calculation</b> — every number comes from the tools, which delegate
 * to the existing backend services. The LLM only turns the pre-computed facts into natural language.
 * When no tool yields data, the assistant honestly reports it cannot answer.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIFinancialAssistantService {

    private static final int HISTORY_TURNS_FOR_PROMPT = 4;

    private final IntentRouter intentRouter;
    private final ToolOrchestrator toolOrchestrator;
    private final PromptBuilder promptBuilder;
    private final LlmProviderResolver providerResolver;
    private final ConversationMemoryService conversationMemory;
    private final AiAssistantProperties properties;

    public ChatResponse chat(User user, String message) {
        long start = System.nanoTime();

        RoutingResult routing = intentRouter.route(message);
        if (properties.isToolLoggingEnabled()) {
            log.info("AI chat: intent={}, tools={}", routing.getIntent(), routing.getToolKeys());
        }

        // Capture prior context, then record this user turn.
        List<ConversationTurn> priorHistory = conversationMemory.recent(user.getId(), HISTORY_TURNS_FOR_PROMPT);
        conversationMemory.append(user.getId(), ConversationMemoryService.ROLE_USER, message);

        Map<String, ToolResult> toolResults = toolOrchestrator.execute(user, routing.getToolKeys());

        LlmPrompt prompt = promptBuilder.build(message, routing.getIntent(), toolResults, priorHistory);

        long llmStart = System.nanoTime();
        LlmResponse llmResponse = providerResolver.resolve().generate(prompt);
        long llmMs = (System.nanoTime() - llmStart) / 1_000_000L;

        conversationMemory.append(user.getId(), ConversationMemoryService.ROLE_ASSISTANT, llmResponse.getText());

        List<String> referencedModules = toolResults.values().stream()
                .filter(ToolResult::isAvailable)
                .map(ToolResult::getModuleLabel)
                .distinct()
                .toList();
        List<String> toolsUsed = toolResults.values().stream()
                .filter(ToolResult::isAvailable)
                .map(ToolResult::getToolKey)
                .toList();

        double confidence = prompt.isHasFacts() ? routing.getConfidence() : 0.2;
        List<String> followUps = routing.getFollowUps().stream().limit(4).toList();
        long totalMs = (System.nanoTime() - start) / 1_000_000L;

        if (properties.isToolLoggingEnabled()) {
            log.info("AI chat done: intent={}, toolsUsed={}, provider={}, llmMs={}, totalMs={}",
                    routing.getIntent(), toolsUsed, llmResponse.getProviderName(), llmMs, totalMs);
        }

        return ChatResponse.builder()
                .assistantMessage(llmResponse.getText())
                .toolsUsed(toolsUsed)
                .referencedModules(referencedModules)
                .intent(routing.getIntent().name())
                .confidence(confidence)
                .suggestedFollowUps(followUps)
                .processingTimeMs(totalMs)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ConversationHistoryResponse getHistory(User user) {
        return ConversationHistoryResponse.builder()
                .turns(conversationMemory.history(user.getId()))
                .build();
    }

    public void clearHistory(User user) {
        conversationMemory.clear(user.getId());
    }
}
