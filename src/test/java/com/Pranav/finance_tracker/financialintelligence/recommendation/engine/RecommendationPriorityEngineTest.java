package com.Pranav.finance_tracker.financialintelligence.recommendation.engine;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationPriorityEngineTest {

    private final RecommendationPriorityEngine engine = new RecommendationPriorityEngine();

    private RecommendationContext context(int health) {
        return RecoFixtures.context(TestFixtures.riskContext().build(), health, "0", "0");
    }

    @Test
    void ranksHigherSavingAndUrgencyFirst() {
        var big = RecoFixtures.draft("BIG", RecommendationType.SAVING, Priority.CRITICAL, "5000", 0.9);
        var small = RecoFixtures.draft("SMALL", RecommendationType.HABIT, Priority.LOW, "200", 0.6);

        List<RecommendationDraft> ordered = engine.prioritize(List.of(small, big), context(50));

        assertThat(ordered).extracting(RecommendationDraft::getRuleKey).containsExactly("BIG", "SMALL");
    }

    @Test
    void scoreIsBoundedAndReflectsInputs() {
        var strong = RecoFixtures.draft("S", RecommendationType.SAVING, Priority.CRITICAL, "5000", 1.0);
        var weak = RecoFixtures.draft("W", RecommendationType.HABIT, Priority.LOW, "0", 0.5);

        double strongScore = engine.score(strong, context(20));
        double weakScore = engine.score(weak, context(90));

        assertThat(strongScore).isBetween(0.0, 100.0).isGreaterThan(weakScore);
        assertThat(weakScore).isBetween(0.0, 100.0);
    }
}
