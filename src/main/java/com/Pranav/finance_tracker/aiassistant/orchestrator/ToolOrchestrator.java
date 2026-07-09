package com.Pranav.finance_tracker.aiassistant.orchestrator;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.FinancialToolRegistry;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes the tools an intent selected and collects their {@link ToolResult}s.
 *
 * <p>Tool keys are de-duplicated first, so within a single request each backend service is called
 * at most once (satisfying "avoid repeated API calls / reuse cached results"). Each tool is
 * isolated: a failure is logged and downgraded to an <i>unavailable</i> result so one flaky tool
 * never fails the whole answer.</p>
 *
 * <p>The selected tools are independent, so they could be run concurrently; they are executed
 * sequentially here to keep a single consistent read view and avoid sharing a persistence context
 * across threads. De-duplication already removes the redundant calls that dominate latency.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolOrchestrator {

    private final FinancialToolRegistry registry;

    /**
     * Runs each distinct tool key against the user's data.
     *
     * @return results keyed by tool key, in request order (missing tools are skipped)
     */
    public Map<String, ToolResult> execute(User user, List<String> toolKeys) {
        Map<String, ToolResult> results = new LinkedHashMap<>();
        List<String> distinct = new ArrayList<>(new java.util.LinkedHashSet<>(toolKeys));

        for (String key : distinct) {
            FinancialTool tool = registry.get(key).orElse(null);
            if (tool == null) {
                log.warn("No tool registered for key '{}'", key);
                continue;
            }
            try {
                results.put(key, tool.fetch(user));
            } catch (Exception ex) {
                // Never leak financial values into logs — record the failing tool only.
                log.error("Tool '{}' failed: {}", key, ex.getMessage());
                results.put(key, ToolResult.unavailable(key, tool.moduleLabel()));
            }
        }
        return results;
    }
}
