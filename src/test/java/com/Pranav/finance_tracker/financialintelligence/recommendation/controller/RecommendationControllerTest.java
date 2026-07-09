package com.Pranav.finance_tracker.financialintelligence.recommendation.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.FinancialRecommendationResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationStatus;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationService;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock private RecommendationService recommendationService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private RecommendationController controller;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        user = TestFixtures.user();
        when(securityUtils.getCurrentUser()).thenReturn(user);
    }

    private FinancialRecommendationResponse response(String title) {
        return FinancialRecommendationResponse.builder()
                .id(UUID.randomUUID())
                .title(title)
                .description("desc")
                .recommendationType(RecommendationType.SAVING)
                .priority(Priority.HIGH)
                .expectedMonthlySaving(new BigDecimal("2000"))
                .confidence(0.8)
                .actionText("go")
                .status(RecommendationStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    void listsActiveRecommendations() throws Exception {
        when(recommendationService.getActiveRecommendations(user)).thenReturn(List.of(response("Boost your savings")));

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Boost your savings"));
    }

    @Test
    void listsTopRecommendations() throws Exception {
        when(recommendationService.getTopRecommendations(user)).thenReturn(List.of(response("Cancel subscription")));

        mockMvc.perform(get("/api/recommendations/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Cancel subscription"));
    }

    @Test
    void listsHistory() throws Exception {
        when(recommendationService.getHistory(user)).thenReturn(List.of(response("Old advice")));

        mockMvc.perform(get("/api/recommendations/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Old advice"));
    }

    @Test
    void dismissesRecommendation() throws Exception {
        UUID id = UUID.randomUUID();
        when(recommendationService.dismiss(eq(user), eq(id))).thenReturn(response("dismissed"));

        mockMvc.perform(put("/api/recommendations/{id}/dismiss", id)).andExpect(status().isOk());

        verify(recommendationService).dismiss(user, id);
    }

    @Test
    void completesRecommendation() throws Exception {
        UUID id = UUID.randomUUID();
        when(recommendationService.complete(eq(user), eq(id))).thenReturn(response("completed"));

        mockMvc.perform(put("/api/recommendations/{id}/complete", id)).andExpect(status().isOk());

        verify(recommendationService).complete(user, id);
    }

    @Test
    void returnsSummary() throws Exception {
        RecommendationSummaryResponse summary = RecommendationSummaryResponse.builder()
                .totalRecommendations(5)
                .activeCount(3)
                .completedCount(1)
                .dismissedCount(1)
                .potentialMonthlySavings(new BigDecimal("4700"))
                .potentialAnnualSavings(new BigDecimal("56400"))
                .highestPriority(response("Prioritise debts"))
                .topRecommendations(List.of(response("Prioritise debts")))
                .recentlyCompleted(List.of())
                .build();
        when(recommendationService.getSummary(user)).thenReturn(summary);

        mockMvc.perform(get("/api/recommendations/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount").value(3))
                .andExpect(jsonPath("$.potentialMonthlySavings").value(4700))
                .andExpect(jsonPath("$.highestPriority.title").value("Prioritise debts"));
    }
}
