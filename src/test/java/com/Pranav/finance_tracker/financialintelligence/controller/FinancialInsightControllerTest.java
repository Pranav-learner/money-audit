package com.Pranav.finance_tracker.financialintelligence.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.FinancialInsightResponse;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.service.FinancialInsightService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinancialInsightControllerTest {

    @Mock private FinancialInsightService insightService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private FinancialInsightController controller;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        user = TestFixtures.user();
        when(securityUtils.getCurrentUser()).thenReturn(user);
    }

    private FinancialInsightResponse response(String title) {
        return FinancialInsightResponse.builder()
                .id(UUID.randomUUID())
                .title(title)
                .description("desc")
                .insightType(InsightType.INFORMATION)
                .severity(Severity.LOW)
                .confidence(0.8)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(14))
                .viewed(false)
                .dismissed(false)
                .build();
    }

    @Test
    void listsActiveInsights() throws Exception {
        when(insightService.getActiveInsights(user)).thenReturn(List.of(response("Food spending increased")));

        mockMvc.perform(get("/api/financial-insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Food spending increased"));
    }

    @Test
    void listsUnreadInsights() throws Exception {
        when(insightService.getUnreadInsights(user)).thenReturn(List.of(response("Budget exceeded: Food")));

        mockMvc.perform(get("/api/financial-insights/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Budget exceeded: Food"));
    }

    @Test
    void marksInsightAsRead() throws Exception {
        UUID id = UUID.randomUUID();
        when(insightService.markAsRead(eq(user), eq(id))).thenReturn(response("read"));

        mockMvc.perform(put("/api/financial-insights/{id}/read", id))
                .andExpect(status().isOk());

        verify(insightService).markAsRead(user, id);
    }

    @Test
    void dismissesInsight() throws Exception {
        UUID id = UUID.randomUUID();
        when(insightService.dismiss(eq(user), eq(id))).thenReturn(response("dismissed"));

        mockMvc.perform(put("/api/financial-insights/{id}/dismiss", id))
                .andExpect(status().isOk());

        verify(insightService).dismiss(user, id);
    }

    @Test
    void returnsSummary() throws Exception {
        InsightSummaryResponse summary = InsightSummaryResponse.builder()
                .totalInsights(4)
                .unreadCount(3)
                .highSeverityCount(1)
                .latestInsight(response("latest"))
                .build();
        when(insightService.getSummary(user)).thenReturn(summary);

        mockMvc.perform(get("/api/financial-insights/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInsights").value(4))
                .andExpect(jsonPath("$.unreadCount").value(3))
                .andExpect(jsonPath("$.highSeverityCount").value(1))
                .andExpect(jsonPath("$.latestInsight.title").value("latest"));
    }
}
