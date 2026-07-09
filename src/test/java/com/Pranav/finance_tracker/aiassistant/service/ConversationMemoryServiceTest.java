package com.Pranav.finance_tracker.aiassistant.service;

import com.Pranav.finance_tracker.aiassistant.config.AiAssistantProperties;
import com.Pranav.finance_tracker.aiassistant.dto.ConversationTurn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMemoryServiceTest {

    private ConversationMemoryService memoryWithSize(int size) {
        AiAssistantProperties props = new AiAssistantProperties();
        props.setConversationHistorySize(size);
        return new ConversationMemoryService(props);
    }

    @Test
    void appendsAndReturnsHistoryOldestFirst() {
        var memory = memoryWithSize(10);
        UUID user = UUID.randomUUID();

        memory.append(user, ConversationMemoryService.ROLE_USER, "hi");
        memory.append(user, ConversationMemoryService.ROLE_ASSISTANT, "hello");

        List<ConversationTurn> history = memory.history(user);
        assertThat(history).extracting(ConversationTurn::getMessage).containsExactly("hi", "hello");
    }

    @Test
    void capsHistoryToTwiceTheConfiguredSize() {
        var memory = memoryWithSize(1); // cap = 2 turns
        UUID user = UUID.randomUUID();

        memory.append(user, "USER", "q1");
        memory.append(user, "ASSISTANT", "a1");
        memory.append(user, "USER", "q2");
        memory.append(user, "ASSISTANT", "a2");

        assertThat(memory.history(user)).extracting(ConversationTurn::getMessage).containsExactly("q2", "a2");
    }

    @Test
    void clearRemovesSession() {
        var memory = memoryWithSize(10);
        UUID user = UUID.randomUUID();
        memory.append(user, "USER", "q");

        memory.clear(user);

        assertThat(memory.history(user)).isEmpty();
    }

    @Test
    void isolatesSessionsPerUser() {
        var memory = memoryWithSize(10);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        memory.append(a, "USER", "a-question");

        assertThat(memory.history(b)).isEmpty();
        assertThat(memory.history(a)).hasSize(1);
    }
}
