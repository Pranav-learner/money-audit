package com.Pranav.finance_tracker.financialintelligence.healthscore.dto;

import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Client-facing view of one component's contribution to the health score. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentScoreResponse {

    private HealthComponent component;
    private int score;
    private int maxPoints;
    private String reason;
}
