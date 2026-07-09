package com.Pranav.finance_tracker.aiassistant.llm;

import com.Pranav.finance_tracker.aiassistant.config.AiAssistantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the active {@link LlmProvider} from configuration ({@code ai.provider}), falling back to
 * the {@link TemplateLlmProvider} when the configured provider is not registered. Centralising this
 * lookup means the orchestrator never hard-codes a vendor.
 */
@Component
@Slf4j
public class LlmProviderResolver {

    private final Map<String, LlmProvider> providersByName;
    private final AiAssistantProperties properties;

    public LlmProviderResolver(List<LlmProvider> providers, AiAssistantProperties properties) {
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(p -> p.name().toLowerCase(), Function.identity()));
        this.properties = properties;
    }

    public LlmProvider resolve() {
        String configured = properties.getProvider() == null ? TemplateLlmProvider.NAME : properties.getProvider().toLowerCase();
        LlmProvider provider = providersByName.get(configured);
        if (provider == null) {
            log.warn("Configured LLM provider '{}' not found; falling back to '{}'", configured, TemplateLlmProvider.NAME);
            provider = providersByName.get(TemplateLlmProvider.NAME);
        }
        return provider;
    }
}
