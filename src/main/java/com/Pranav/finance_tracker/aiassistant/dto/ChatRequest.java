package com.Pranav.finance_tracker.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for {@code POST /api/ai/chat}. */
@Data
public class ChatRequest {

    @NotBlank
    @Size(max = 1000)
    private String message;
}
