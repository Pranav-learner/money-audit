package com.Pranav.finance_tracker.financialintelligence.healthscore.dto;

import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import lombok.Builder;
import lombok.Getter;

/** One component's contribution to the overall health score, with a plain-language reason. */
@Getter
@Builder
public class ComponentScore {

    private final HealthComponent component;

    /** Points awarded, in [0, {@link #maxPoints}]. */
    private final int score;

    /** Maximum points this component can contribute (its weight). */
    private final int maxPoints;

    /** Why this component scored as it did. */
    private final String reason;

    /** Fraction of the maximum achieved, in [0, 1]. */
    public double fraction() {
        return maxPoints <= 0 ? 0 : (double) score / maxPoints;
    }
}
