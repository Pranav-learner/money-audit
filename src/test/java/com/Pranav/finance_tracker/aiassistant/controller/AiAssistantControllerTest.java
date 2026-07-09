package com.Pranav.finance_tracker.aiassistant.controller;

import com.Pranav.finance_tracker.aiassistant.dto.ChatResponse;
import com.Pranav.finance_tracker.aiassistant.dto.ConversationHistoryResponse;
import com.Pranav.finance_tracker.aiassistant.dto.ConversationTurn;
import com.Pranav.finance_tracker.aiassistant.service.AIFinancialAssistantService;
import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiAssistantControllerTest {

    @Mock private AIFinancialAssistantService assistantService;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private AiAssistantController controller;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        user = TestFixtures.user();
        org.mockito.Mockito.lenient().when(securityUtils.getCurrentUser()).thenReturn(user);
    }

    @Test
    void chatReturnsAssistantResponse() throws Exception {
        ChatResponse response = ChatResponse.builder()
                .assistantMessage("Your Food budget is at 93%.")
                .toolsUsed(List.of("budget")).referencedModules(List.of("Budgets"))
                .intent("BUDGET_ANALYSIS").confidence(0.8)
                .suggestedFollowUps(List.of("Want tips to stay within budget?"))
                .processingTimeMs(12).timestamp(LocalDateTime.now()).build();
        when(assistantService.chat(eq(user), any())).thenReturn(response);

        mockMvc.perform(post("/api/ai/chat").contentType("application/json")
                        .content("{\"message\":\"Which budget is closest to being exceeded?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantMessage").value("Your Food budget is at 93%."))
                .andExpect(jsonPath("$.intent").value("BUDGET_ANALYSIS"))
                .andExpect(jsonPath("$.toolsUsed[0]").value("budget"));
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/ai/chat").contentType("application/json").content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsHistory() throws Exception {
        when(assistantService.getHistory(user)).thenReturn(ConversationHistoryResponse.builder()
                .turns(List.of(ConversationTurn.builder().role("USER").message("hi").timestamp(LocalDateTime.now()).build()))
                .build());

        mockMvc.perform(get("/api/ai/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turns[0].message").value("hi"));
    }

    @Test
    void clearsHistory() throws Exception {
        mockMvc.perform(delete("/api/ai/history")).andExpect(status().isNoContent());
        verify(assistantService).clearHistory(user);
    }
}
