package com.Pranav.finance_tracker.financialintelligence.healthscore.dto;

import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A single point in the health-score history/trend. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScorePointResponse {

    private int score;
    private HealthBand band;
    private LocalDateTime createdAt;
}
