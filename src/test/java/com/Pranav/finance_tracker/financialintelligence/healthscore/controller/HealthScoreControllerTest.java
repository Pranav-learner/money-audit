package com.Pranav.finance_tracker.financialintelligence.healthscore.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScorePointResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScoreResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthBand;
import com.Pranav.finance_tracker.financialintelligence.healthscore.service.HealthScoreService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HealthScoreControllerTest {

    @Mock private HealthScoreService healthScoreService;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private HealthScoreController controller;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        user = TestFixtures.user();
        org.mockito.Mockito.lenient().when(securityUtils.getCurrentUser()).thenReturn(user);
    }

    @Test
    void returnsCurrentScore() throws Exception {
        HealthScoreResponse response = HealthScoreResponse.builder()
                .score(72).band(HealthBand.GOOD).components(List.of())
                .explanation("Your financial health score is 72/100 (GOOD).")
                .changeSincePrevious(-8).changeExplanation("Your score declined by 8 points, mainly because savings behavior got weaker.")
                .generatedAt(LocalDateTime.now()).build();
        when(healthScoreService.getCurrentScore(user)).thenReturn(response);

        mockMvc.perform(get("/api/health-score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(72))
                .andExpect(jsonPath("$.band").value("GOOD"))
                .andExpect(jsonPath("$.changeSincePrevious").value(-8));
    }

    @Test
    void returnsHistory() throws Exception {
        when(healthScoreService.getHistory(eq(user), eq(30))).thenReturn(List.of(
                HealthScorePointResponse.builder().score(72).band(HealthBand.GOOD).createdAt(LocalDateTime.now()).build()));

        mockMvc.perform(get("/api/health-score/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(72));
    }
}
