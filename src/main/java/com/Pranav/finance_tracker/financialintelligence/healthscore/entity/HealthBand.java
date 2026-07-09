package com.Pranav.finance_tracker.financialintelligence.healthscore.entity;

/**
 * A qualitative band for a numeric financial health score, used for colour/labelling and messaging.
 */
public enum HealthBand {

    EXCELLENT(80),
    GOOD(60),
    FAIR(40),
    NEEDS_ATTENTION(20),
    CRITICAL(0);

    private final int minScore;

    HealthBand(int minScore) {
        this.minScore = minScore;
    }

    /** Maps a 0–100 score to its band. */
    public static HealthBand fromScore(int score) {
        for (HealthBand band : values()) {
            if (score >= band.minScore) {
                return band;
            }
        }
        return CRITICAL;
    }
}
