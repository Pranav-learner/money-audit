package com.Pranav.finance_tracker.financialintelligence.healthscore.dto;

import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthBand;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** The full output of the {@code HealthScoreEngine}: overall score, band, breakdown and explanation. */
@Getter
@Builder
public class HealthScoreResult {

    private final int overallScore;
    private final HealthBand band;
    private final List<ComponentScore> components;
    private final String explanation;
}
