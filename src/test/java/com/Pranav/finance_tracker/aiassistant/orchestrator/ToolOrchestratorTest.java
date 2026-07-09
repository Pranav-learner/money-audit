package com.Pranav.finance_tracker.aiassistant.orchestrator;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.FinancialToolRegistry;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ToolOrchestratorTest {

    private final User user = TestFixtures.user();

    private FinancialTool tool(String key, AtomicInteger counter, boolean explode) {
        return new FinancialTool() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public String moduleLabel() {
                return key.toUpperCase();
            }

            @Override
            public ToolResult fetch(User u) {
                counter.incrementAndGet();
                if (explode) {
                    throw new IllegalStateException("boom");
                }
                return ToolResult.of(key, moduleLabel(), "data for " + key);
            }
        };
    }

    @Test
    void runsEachDistinctToolOnce() {
        AtomicInteger aCalls = new AtomicInteger();
        var registry = new FinancialToolRegistry(List.of(tool("a", aCalls, false)));
        var orchestrator = new ToolOrchestrator(registry);

        // "a" requested twice → executed once.
        Map<String, ToolResult> results = orchestrator.execute(user, List.of("a", "a"));

        assertThat(aCalls.get()).isEqualTo(1);
        assertThat(results.get("a").isAvailable()).isTrue();
    }

    @Test
    void isolatesAFailingTool() {
        var registry = new FinancialToolRegistry(List.of(
                tool("ok", new AtomicInteger(), false), tool("bad", new AtomicInteger(), true)));
        var orchestrator = new ToolOrchestrator(registry);

        Map<String, ToolResult> results = orchestrator.execute(user, List.of("ok", "bad"));

        assertThat(results.get("ok").isAvailable()).isTrue();
        assertThat(results.get("bad").isAvailable()).isFalse();
    }

    @Test
    void skipsUnknownToolKeys() {
        var orchestrator = new ToolOrchestrator(new FinancialToolRegistry(List.of()));
        assertThat(orchestrator.execute(user, List.of("missing"))).isEmpty();
    }
}
