package com.Pranav.finance_tracker.aiassistant.llm;

import lombok.Builder;
import lombok.Getter;

/** The text an {@link LlmProvider} produced, plus which provider produced it. */
@Getter
@Builder
public class LlmResponse {

    private final String text;
    private final String providerName;
}
