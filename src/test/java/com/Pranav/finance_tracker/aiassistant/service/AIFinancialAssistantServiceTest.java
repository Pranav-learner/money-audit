package com.Pranav.finance_tracker.aiassistant.service;

import com.Pranav.finance_tracker.aiassistant.config.AiAssistantProperties;
import com.Pranav.finance_tracker.aiassistant.dto.ChatResponse;
import com.Pranav.finance_tracker.aiassistant.llm.LlmProvider;
import com.Pranav.finance_tracker.aiassistant.llm.LlmProviderResolver;
import com.Pranav.finance_tracker.aiassistant.llm.LlmResponse;
import com.Pranav.finance_tracker.aiassistant.llm.TemplateLlmProvider;
import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.orchestrator.IntentRouter;
import com.Pranav.finance_tracker.aiassistant.orchestrator.RoutingResult;
import com.Pranav.finance_tracker.aiassistant.orchestrator.ToolOrchestrator;
import com.Pranav.finance_tracker.aiassistant.prompts.LlmPrompt;
import com.Pranav.finance_tracker.aiassistant.prompts.PromptBuilder;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIFinancialAssistantServiceTest {

    @Mock private IntentRouter intentRouter;
    @Mock private ToolOrchestrator toolOrchestrator;
    @Mock private PromptBuilder promptBuilder;
    @Mock private LlmProviderResolver providerResolver;
    @Mock private ConversationMemoryService conversationMemory;
    @Mock private LlmProvider llmProvider;

    private AIFinancialAssistantService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AIFinancialAssistantService(intentRouter, toolOrchestrator, promptBuilder,
                providerResolver, conversationMemory, new AiAssistantProperties());
        user = TestFixtures.user();
        org.mockito.Mockito.lenient().when(providerResolver.resolve()).thenReturn(llmProvider);
        org.mockito.Mockito.lenient().when(conversationMemory.recent(any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());
    }

    private RoutingResult routing() {
        return RoutingResult.builder()
                .intent(Intent.BUDGET_ANALYSIS).toolKeys(List.of("budget"))
                .followUps(List.of("Follow up 1?", "Follow up 2?")).confidence(0.8).build();
    }

    @Test
    void answersUsingToolFactsAndRecordsBothTurns() {
        when(intentRouter.route("How are my budgets?")).thenReturn(routing());
        when(toolOrchestrator.execute(eq(user), any()))
                .thenReturn(Map.of("budget", ToolResult.of("budget", "Budgets", "Food is at 93%.")));
        when(promptBuilder.build(any(), any(), any(), any()))
                .thenReturn(LlmPrompt.builder().hasFacts(true).factContext("Food is at 93%.").build());
        when(llmProvider.generate(any()))
                .thenReturn(LlmResponse.builder().text("Your Food budget is at 93%.").providerName("template").build());

        ChatResponse response = service.chat(user, "How are my budgets?");

        assertThat(response.getAssistantMessage()).isEqualTo("Your Food budget is at 93%.");
        assertThat(response.getIntent()).isEqualTo("BUDGET_ANALYSIS");
        assertThat(response.getToolsUsed()).containsExactly("budget");
        assertThat(response.getReferencedModules()).containsExactly("Budgets");
        assertThat(response.getConfidence()).isEqualTo(0.8);
        assertThat(response.getSuggestedFollowUps()).hasSize(2);

        verify(conversationMemory).append(user.getId(), ConversationMemoryService.ROLE_USER, "How are my budgets?");
        verify(conversationMemory).append(user.getId(), ConversationMemoryService.ROLE_ASSISTANT, "Your Food budget is at 93%.");
    }

    @Test
    void lowConfidenceWhenNoFactsAreAvailable() {
        when(intentRouter.route(any())).thenReturn(routing());
        when(toolOrchestrator.execute(eq(user), any()))
                .thenReturn(Map.of("budget", ToolResult.unavailable("budget", "Budgets")));
        when(promptBuilder.build(any(), any(), any(), any()))
                .thenReturn(LlmPrompt.builder().hasFacts(false).factContext("").build());
        when(llmProvider.generate(any()))
                .thenReturn(LlmResponse.builder().text(TemplateLlmProvider.NO_DATA_MESSAGE).providerName("template").build());

        ChatResponse response = service.chat(user, "anything");

        assertThat(response.getConfidence()).isEqualTo(0.2);
        assertThat(response.getAssistantMessage()).isEqualTo(TemplateLlmProvider.NO_DATA_MESSAGE);
        assertThat(response.getReferencedModules()).isEmpty();
    }

    @Test
    void clearHistoryDelegatesToMemory() {
        service.clearHistory(user);
        verify(conversationMemory).clear(user.getId());
    }
}
