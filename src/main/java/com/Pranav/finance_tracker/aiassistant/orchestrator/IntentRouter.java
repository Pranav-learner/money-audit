package com.Pranav.finance_tracker.aiassistant.orchestrator;

import com.Pranav.finance_tracker.aiassistant.orchestrator.strategy.IntentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Classifies a user message into an {@link Intent} and selects the tools to serve it.
 *
 * <p>Injects every {@link IntentStrategy} bean and picks the highest-scoring one (Strategy pattern).
 * When no strategy matches, it falls back to {@link Intent#GENERAL_FINANCE}; a blank message maps to
 * {@link Intent#UNKNOWN}. The keyword heuristics here can be swapped for an ML/LLM classifier by
 * replacing the strategies — the router, tools and orchestrator are unaffected.</p>
 */
@Component
@Slf4j
public class IntentRouter {

    private final List<IntentStrategy> strategies;
    private final IntentStrategy generalFinance;

    public IntentRouter(List<IntentStrategy> strategies) {
        this.strategies = strategies;
        this.generalFinance = strategies.stream()
                .filter(s -> s.intent() == Intent.GENERAL_FINANCE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A GENERAL_FINANCE strategy is required"));
    }

    public RoutingResult route(String message) {
        String normalized = message == null ? "" : message.toLowerCase().trim();
        if (normalized.isBlank()) {
            return RoutingResult.builder()
                    .intent(Intent.UNKNOWN).toolKeys(List.of()).followUps(List.of()).confidence(0.0).build();
        }

        IntentStrategy best = null;
        int bestScore = 0;
        for (IntentStrategy strategy : strategies) {
            if (strategy.intent() == Intent.GENERAL_FINANCE) {
                continue; // fallback only
            }
            int score = strategy.score(normalized);
            if (score > bestScore) {
                bestScore = score;
                best = strategy;
            }
        }

        IntentStrategy chosen = best != null ? best : generalFinance;
        double confidence = best != null ? Math.min(0.95, 0.55 + bestScore * 0.12) : 0.5;

        return RoutingResult.builder()
                .intent(chosen.intent())
                .toolKeys(chosen.toolKeys())
                .followUps(chosen.followUps())
                .confidence(confidence)
                .build();
    }
}
