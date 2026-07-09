package com.Pranav.finance_tracker.financialintelligence.healthscore.dto;

import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Client-facing current health score with its breakdown and the change since the previous score
 * (which answers "why did my Financial Health Score decrease?").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreResponse {

    private int score;
    private HealthBand band;
    private List<ComponentScoreResponse> components;
    private String explanation;

    /** Change versus the previous score (positive = improved, negative = declined); null if no prior score. */
    private Integer changeSincePrevious;

    /** Explanation of the change, e.g. which component moved most. */
    private String changeExplanation;

    private LocalDateTime generatedAt;
}
