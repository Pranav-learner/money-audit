package com.Pranav.finance_tracker.aiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externally-tunable configuration for the AI Financial Assistant, bound from {@code ai.*} in
 * {@code application.yml}. Swapping the {@code provider} lets a deployment change LLM vendor without
 * touching any orchestration or business code (Adapter pattern).
 */
@Component
@ConfigurationProperties(prefix = "ai")
@Data
public class AiAssistantProperties {

    /** LLM provider key, resolved against the registered {@code LlmProvider} beans (e.g. {@code template}, {@code openai}, {@code anthropic}). */
    private String provider = "template";

    /** Model name passed to the provider. */
    private String model = "money-audit-assistant";

    /** Sampling temperature for generative providers. */
    private double temperature = 0.2;

    /** Maximum tokens the provider may generate. */
    private int maxTokens = 600;

    /** Provider request timeout in milliseconds. */
    private long requestTimeoutMs = 15000;

    /** Number of recent turns kept in per-session conversation memory. */
    private int conversationHistorySize = 10;

    /** Whether to log the tools invoked per request (never logs financial values). */
    private boolean toolLoggingEnabled = true;
}
