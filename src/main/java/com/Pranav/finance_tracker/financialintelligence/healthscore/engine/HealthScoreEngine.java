package com.Pranav.finance_tracker.financialintelligence.healthscore.engine;

import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScoreResult;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthBand;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.HealthComponentCalculator;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Computes an explainable financial health score by combining every {@link HealthComponentCalculator}.
 *
 * <p>Spring injects all calculator beans (Strategy pattern), so adding or swapping a component — a
 * heuristic or a future ML sub-model — requires no change here. A failing calculator is isolated
 * (scored 0) so the overall score is always produced. The result is clamped to [0, 100], mapped to a
 * {@link HealthBand}, and given a plain-language explanation naming the strongest and weakest areas.</p>
 */
@Component
@Slf4j
public class HealthScoreEngine {

    private final List<HealthComponentCalculator> calculators;

    public HealthScoreEngine(List<HealthComponentCalculator> calculators) {
        this.calculators = calculators;
        log.info("HealthScoreEngine initialised with {} component(s): {}", calculators.size(),
                calculators.stream().map(c -> c.component().name()).toList());
    }

    public HealthScoreResult evaluate(InsightContext context) {
        List<ComponentScore> components = new ArrayList<>();
        for (HealthComponentCalculator calculator : calculators) {
            try {
                components.add(calculator.evaluate(context));
            } catch (Exception ex) {
                log.error("Health component '{}' failed: {}", calculator.component(), ex.getMessage(), ex);
                components.add(ComponentScore.builder()
                        .component(calculator.component()).maxPoints(0).score(0)
                        .reason("Could not be evaluated.").build());
            }
        }

        int overall = clamp(components.stream().mapToInt(ComponentScore::getScore).sum());
        HealthBand band = HealthBand.fromScore(overall);

        return HealthScoreResult.builder()
                .overallScore(overall)
                .band(band)
                .components(components)
                .explanation(buildExplanation(overall, band, components))
                .build();
    }

    private String buildExplanation(int overall, HealthBand band, List<ComponentScore> components) {
        List<ComponentScore> scored = components.stream()
                .filter(c -> c.getMaxPoints() > 0)
                .sorted(Comparator.comparingDouble(ComponentScore::fraction))
                .toList();
        if (scored.isEmpty()) {
            return String.format("Your financial health score is %d/100 (%s).", overall, band);
        }
        ComponentScore weakest = scored.get(0);
        ComponentScore strongest = scored.get(scored.size() - 1);
        return String.format("Your financial health score is %d/100 (%s). Strongest area: %s. "
                        + "Biggest opportunity: %s — %s",
                overall, band, humanize(strongest.getComponent().name()),
                humanize(weakest.getComponent().name()), weakest.getReason());
    }

    private String humanize(String enumName) {
        return enumName.toLowerCase().replace('_', ' ');
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
