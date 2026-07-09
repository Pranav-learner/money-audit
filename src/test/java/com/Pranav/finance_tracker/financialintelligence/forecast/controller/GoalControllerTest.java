package com.Pranav.finance_tracker.financialintelligence.forecast.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.FinancialGoalResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalForecastResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalPlanResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalStatus;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalType;
import com.Pranav.finance_tracker.financialintelligence.forecast.service.GoalService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
class GoalControllerTest {

    @Mock private GoalService goalService;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private GoalController controller;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        user = TestFixtures.user();
        when(securityUtils.getCurrentUser()).thenReturn(user);
    }

    private FinancialGoalResponse goalResponse() {
        return FinancialGoalResponse.builder()
                .id(UUID.randomUUID()).title("Laptop").goalType(GoalType.GADGET)
                .targetAmount(new BigDecimal("120000")).currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(8)).monthlyContributionRequired(new BigDecimal("15000"))
                .status(GoalStatus.ACTIVE).progressPercent(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void createsGoal() throws Exception {
        when(goalService.createGoal(eq(user), any())).thenReturn(goalResponse());
        String body = "{\"title\":\"Laptop\",\"goalType\":\"GADGET\",\"targetAmount\":120000,"
                + "\"targetDate\":\"" + LocalDate.now().plusMonths(8) + "\"}";

        mockMvc.perform(post("/api/goals").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Laptop"))
                .andExpect(jsonPath("$.monthlyContributionRequired").value(15000));
    }

    @Test
    void listsActiveGoals() throws Exception {
        when(goalService.getActiveGoals(user)).thenReturn(List.of(goalResponse()));

        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Laptop"));
    }

    @Test
    void returnsGoalPlan() throws Exception {
        UUID id = UUID.randomUUID();
        GoalPlanResponse plan = GoalPlanResponse.builder()
                .goalId(id).goalTitle("Laptop").requiredMonthlySaving(new BigDecimal("15000"))
                .feasible(true).successProbability(0.9).monthsRemaining(8)
                .recommendedActions(List.of()).summary("Save ₹15,000 a month.").build();
        when(goalService.getPlan(eq(user), eq(id))).thenReturn(plan);

        mockMvc.perform(get("/api/goals/{id}/plan", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredMonthlySaving").value(15000))
                .andExpect(jsonPath("$.feasible").value(true));
    }

    @Test
    void returnsGoalForecast() throws Exception {
        UUID id = UUID.randomUUID();
        GoalForecastResponse forecast = GoalForecastResponse.builder()
                .goalId(id).successProbability(0.75).projectedCompletionDate(LocalDate.now().plusMonths(9))
                .requiredMonthlySaving(new BigDecimal("15000")).recommendations(List.of("Save more")).build();
        when(goalService.getGoalForecast(eq(user), eq(id))).thenReturn(forecast);

        mockMvc.perform(get("/api/goals/{id}/forecast", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successProbability").value(0.75));
    }

    @Test
    void deletesGoal() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/goals/{id}", id)).andExpect(status().isNoContent());

        verify(goalService).deleteGoal(user, id);
    }
}
