package com.Pranav.finance_tracker.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response for {@code GET /api/ai/history}: the current session's turns. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistoryResponse {

    private List<ConversationTurn> turns;
}
