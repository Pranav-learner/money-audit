package com.Pranav.finance_tracker.financialintelligence.recommendation.engine;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.RecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEngineTest {

    private RecommendationContext context() {
        return RecoFixtures.context(TestFixtures.riskContext().build(), 50, "0", "0");
    }

    private RecommendationRule stub(String key, List<RecommendationDraft> result, boolean explode) {
        return new RecommendationRule() {
            @Override
            public String ruleKey() {
                return key;
            }

            @Override
            public RecommendationType type() {
                return RecommendationType.SPENDING;
            }

            @Override
            public List<RecommendationDraft> evaluate(RecommendationContext context) {
                if (explode) {
                    throw new IllegalStateException("boom");
                }
                return result;
            }
        };
    }

    @Test
    void aggregatesDraftsFromAllRules() {
        var a = RecoFixtures.draft("A1", RecommendationType.SAVING, Priority.MEDIUM, "500", 0.8);
        var b = RecoFixtures.draft("B1", RecommendationType.DEBT, Priority.HIGH, "0", 0.9);

        RecommendationEngine engine = new RecommendationEngine(List.of(stub("A", List.of(a), false), stub("B", List.of(b), false)));
        List<RecommendationDraft> drafts = engine.generate(context());

        assertThat(drafts).extracting(RecommendationDraft::getRuleKey).containsExactly("A1", "B1");
    }

    @Test
    void isolatesAFailingRule() {
        var ok = RecoFixtures.draft("OK1", RecommendationType.SAVING, Priority.LOW, "200", 0.7);

        RecommendationEngine engine = new RecommendationEngine(List.of(stub("BOOM", List.of(), true), stub("OK", List.of(ok), false)));
        List<RecommendationDraft> drafts = engine.generate(context());

        assertThat(drafts).extracting(RecommendationDraft::getRuleKey).containsExactly("OK1");
    }
}
