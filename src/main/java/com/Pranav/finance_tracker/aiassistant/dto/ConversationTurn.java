package com.Pranav.finance_tracker.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A single turn in the lightweight, in-memory conversation history. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationTurn {

    /** {@code USER} or {@code ASSISTANT}. */
    private String role;

    private String message;

    private LocalDateTime timestamp;
}
