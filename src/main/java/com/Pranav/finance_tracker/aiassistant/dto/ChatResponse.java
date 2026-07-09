package com.Pranav.finance_tracker.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Response for {@code POST /api/ai/chat}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** The assistant's natural-language answer. */
    private String assistantMessage;

    /** Keys of the tools invoked to answer (e.g. {@code budget}, {@code forecast}). */
    private List<String> toolsUsed;

    /** Business modules the answer drew on (e.g. {@code Analytics}, {@code Risk}). */
    private List<String> referencedModules;

    /** Detected intent, for transparency. */
    private String intent;

    /** Overall confidence in the answer, in [0.0, 1.0]. */
    private double confidence;

    /** 2–4 suggested follow-up questions. */
    private List<String> suggestedFollowUps;

    /** Wall-clock processing time in milliseconds. */
    private long processingTimeMs;

    private LocalDateTime timestamp;
}
