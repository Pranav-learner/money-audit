package com.Pranav.finance_tracker.financialintelligence.forecast.engine;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.ForecastFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.FinancialPredictor;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ForecastEngineTest {

    private ForecastContext context() {
        return ForecastFixtures.context(
                ForecastFixtures.insight(LocalDate.of(2026, 7, 20), List.of(), List.of(), null, 0),
                "0", "0", 60);
    }

    private FinancialPredictor predictor(ForecastType type, ForecastDraft result, boolean explode) {
        return new FinancialPredictor() {
            @Override
            public ForecastType type() {
                return type;
            }

            @Override
            public ForecastDraft predict(ForecastContext context) {
                if (explode) {
                    throw new IllegalStateException("boom");
                }
                return result;
            }
        };
    }

    private ForecastDraft draft(ForecastType type) {
        return ForecastDraft.builder()
                .forecastType(type).predictedValue(BigDecimal.TEN).confidence(0.7)
                .predictionDate(LocalDate.of(2026, 7, 31)).predictionPeriod("2026-07")
                .explanation("x").build();
    }

    @Test
    void collectsForecastsSkippingNulls() {
        var engine = new ForecastEngine(List.of(
                predictor(ForecastType.MONTHLY_SPENDING, draft(ForecastType.MONTHLY_SPENDING), false),
                predictor(ForecastType.DEBT, null, false)));

        List<ForecastDraft> forecasts = engine.generate(context());

        assertThat(forecasts).extracting(ForecastDraft::getForecastType).containsExactly(ForecastType.MONTHLY_SPENDING);
    }

    @Test
    void isolatesAFailingPredictor() {
        var engine = new ForecastEngine(List.of(
                predictor(ForecastType.CASHFLOW, null, true),
                predictor(ForecastType.NET_WORTH, draft(ForecastType.NET_WORTH), false)));

        List<ForecastDraft> forecasts = engine.generate(context());

        assertThat(forecasts).extracting(ForecastDraft::getForecastType).containsExactly(ForecastType.NET_WORTH);
    }
}
