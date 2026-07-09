package com.Pranav.finance_tracker.aiassistant.llm;

import com.Pranav.finance_tracker.aiassistant.config.AiAssistantProperties;
import com.Pranav.finance_tracker.aiassistant.prompts.LlmPrompt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmProviderTest {

    private final TemplateLlmProvider template = new TemplateLlmProvider();

    private LlmPrompt prompt(String facts, boolean hasFacts) {
        return LlmPrompt.builder()
                .systemPrompt("sys").userQuestion("q").factContext(facts)
                .recentHistory(List.of()).hasFacts(hasFacts).build();
    }

    @Test
    void templateComposesAnswerFromFacts() {
        var response = template.generate(prompt("Food is at 93% of budget.", true));
        assertThat(response.getProviderName()).isEqualTo("template");
        assertThat(response.getText()).contains("Food is at 93%");
    }

    @Test
    void templateReturnsNoDataMessageWhenNoFacts() {
        var response = template.generate(prompt("", false));
        assertThat(response.getText()).isEqualTo(TemplateLlmProvider.NO_DATA_MESSAGE);
    }

    @Test
    void resolverReturnsConfiguredProvider() {
        AiAssistantProperties props = new AiAssistantProperties();
        props.setProvider("template");
        var resolver = new LlmProviderResolver(List.of(template), props);

        assertThat(resolver.resolve()).isSameAs(template);
    }

    @Test
    void resolverFallsBackToTemplateForUnknownProvider() {
        AiAssistantProperties props = new AiAssistantProperties();
        props.setProvider("openai"); // not registered
        var resolver = new LlmProviderResolver(List.of(template), props);

        assertThat(resolver.resolve()).isSameAs(template);
    }
}
