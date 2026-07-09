package com.Pranav.finance_tracker.financialintelligence.forecast.service;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.ForecastFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.FinancialForecast;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.engine.ForecastEngine;
import com.Pranav.finance_tracker.financialintelligence.forecast.repository.FinancialForecastRepository;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock private ForecastContextFactory contextFactory;
    @Mock private ForecastEngine forecastEngine;
    @Mock private FinancialForecastRepository forecastRepository;

    private ForecastService service;
    private User user;
    private InsightContext insight;

    @BeforeEach
    void setUp() {
        service = new ForecastService(contextFactory, forecastEngine, forecastRepository);
        user = TestFixtures.user();
        insight = TestFixtures.riskContext().build();
    }

    private ForecastDraft draft(ForecastType type) {
        return ForecastDraft.builder()
                .forecastType(type).predictedValue(BigDecimal.TEN).confidence(0.7)
                .predictionDate(LocalDate.now()).predictionPeriod("2026-07").explanation("x").build();
    }

    @Test
    void generatesAndPersistsNewForecasts() {
        when(contextFactory.build(eq(user), any())).thenReturn(ForecastFixtures.context(insight, "0", "0", 60));
        when(forecastEngine.generate(any())).thenReturn(List.of(draft(ForecastType.MONTHLY_SPENDING), draft(ForecastType.DEBT)));
        when(forecastRepository.existsByUserIdAndForecastTypeAndCreatedAtBetween(any(), any(), any(), any())).thenReturn(false);

        int created = service.generateForUser(user, insight);

        assertThat(created).isEqualTo(2);
        ArgumentCaptor<List<FinancialForecast>> captor = ArgumentCaptor.forClass(List.class);
        verify(forecastRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void skipsForecastsAlreadyGeneratedToday() {
        when(contextFactory.build(eq(user), any())).thenReturn(ForecastFixtures.context(insight, "0", "0", 60));
        when(forecastEngine.generate(any())).thenReturn(List.of(draft(ForecastType.MONTHLY_SPENDING), draft(ForecastType.DEBT)));
        when(forecastRepository.existsByUserIdAndForecastTypeAndCreatedAtBetween(any(), eq(ForecastType.MONTHLY_SPENDING), any(), any())).thenReturn(true);
        when(forecastRepository.existsByUserIdAndForecastTypeAndCreatedAtBetween(any(), eq(ForecastType.DEBT), any(), any())).thenReturn(false);

        int created = service.generateForUser(user, insight);

        assertThat(created).isEqualTo(1);
    }

    @Test
    void summaryReturnsLatestPerType() {
        FinancialForecast spending = forecast(ForecastType.MONTHLY_SPENDING, LocalDateTime.now());
        FinancialForecast debt = forecast(ForecastType.DEBT, LocalDateTime.now().minusHours(1));
        when(forecastRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(spending, debt));

        var summary = service.getSummary(user);

        assertThat(summary.getSpendingForecast()).isNotNull();
        assertThat(summary.getDebtForecast()).isNotNull();
        assertThat(summary.getSavingsForecast()).isNull();
    }

    private FinancialForecast forecast(ForecastType type, LocalDateTime createdAt) {
        return FinancialForecast.builder()
                .id(java.util.UUID.randomUUID()).userId(user.getId()).forecastType(type)
                .predictedValue(BigDecimal.TEN).confidence(0.7).predictionDate(LocalDate.now())
                .predictionPeriod("2026-07").explanation("x").createdAt(createdAt).build();
    }
}
