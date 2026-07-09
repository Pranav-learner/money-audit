package com.Pranav.finance_tracker.aiassistant.service;

import com.Pranav.finance_tracker.aiassistant.config.AiAssistantProperties;
import com.Pranav.finance_tracker.aiassistant.dto.ConversationTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight, in-memory conversation memory, keyed by user (the "session").
 *
 * <p>Keeps only the most recent turns (capped by {@code ai.conversation-history-size}) for
 * continuity within a session. It is intentionally <b>ephemeral</b> — nothing is persisted and no
 * long-term memory is kept, per the module's requirements. Access is thread-safe.</p>
 */
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";

    private final AiAssistantProperties properties;
    private final Map<UUID, Deque<ConversationTurn>> sessions = new ConcurrentHashMap<>();

    public void append(UUID userId, String role, String message) {
        Deque<ConversationTurn> turns = sessions.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (turns) {
            turns.addLast(ConversationTurn.builder()
                    .role(role).message(message).timestamp(LocalDateTime.now()).build());
            // Keep user+assistant pairs: cap at 2 × configured size.
            int maxTurns = Math.max(2, properties.getConversationHistorySize() * 2);
            while (turns.size() > maxTurns) {
                turns.removeFirst();
            }
        }
    }

    /** Full retained history for the session, oldest first. */
    public List<ConversationTurn> history(UUID userId) {
        Deque<ConversationTurn> turns = sessions.get(userId);
        if (turns == null) {
            return List.of();
        }
        synchronized (turns) {
            return new ArrayList<>(turns);
        }
    }

    /** The most recent {@code n} turns, oldest first. */
    public List<ConversationTurn> recent(UUID userId, int n) {
        List<ConversationTurn> all = history(userId);
        if (all.size() <= n) {
            return all;
        }
        return new ArrayList<>(all.subList(all.size() - n, all.size()));
    }

    public void clear(UUID userId) {
        sessions.remove(userId);
    }
}
