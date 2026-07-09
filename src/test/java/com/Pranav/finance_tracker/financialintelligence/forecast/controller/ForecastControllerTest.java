package com.Pranav.finance_tracker.financialintelligence.forecast.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.FinancialForecastResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.service.ForecastService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ForecastControllerTest {

    @Mock private ForecastService forecastService;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private ForecastController controller;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        user = TestFixtures.user();
        when(securityUtils.getCurrentUser()).thenReturn(user);
    }

    private FinancialForecastResponse forecast(ForecastType type) {
        return FinancialForecastResponse.builder()
                .id(UUID.randomUUID()).forecastType(type).predictedValue(new BigDecimal("12400"))
                .confidence(0.8).predictionDate(LocalDate.now()).predictionPeriod("2026-07")
                .explanation("on track").createdAt(LocalDateTime.now()).build();
    }

    @Test
    void listsForecasts() throws Exception {
        when(forecastService.getForecasts(user)).thenReturn(List.of(forecast(ForecastType.MONTHLY_SPENDING)));

        mockMvc.perform(get("/api/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].forecastType").value("MONTHLY_SPENDING"));
    }

    @Test
    void returnsSummary() throws Exception {
        ForecastSummaryResponse summary = ForecastSummaryResponse.builder()
                .spendingForecast(forecast(ForecastType.MONTHLY_SPENDING))
                .debtForecast(forecast(ForecastType.DEBT))
                .build();
        when(forecastService.getSummary(user)).thenReturn(summary);

        mockMvc.perform(get("/api/forecast/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spendingForecast.predictedValue").value(12400))
                .andExpect(jsonPath("$.savingsForecast").doesNotExist());
    }
}
