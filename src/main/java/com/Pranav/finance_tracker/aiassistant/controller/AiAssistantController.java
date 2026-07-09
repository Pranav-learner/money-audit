package com.Pranav.finance_tracker.aiassistant.controller;

import com.Pranav.finance_tracker.aiassistant.dto.ChatRequest;
import com.Pranav.finance_tracker.aiassistant.dto.ChatResponse;
import com.Pranav.finance_tracker.aiassistant.dto.ConversationHistoryResponse;
import com.Pranav.finance_tracker.aiassistant.service.AIFinancialAssistantService;
import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Conversational interface to the Financial Intelligence Platform.
 *
 * <p>Every endpoint is JWT-protected (via the global security rule) and operates only on the
 * authenticated user's own data — the assistant never receives another user's information and never
 * exposes internal identifiers or database structure.</p>
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Conversational financial assistant over the platform's engines")
public class AiAssistantController {

    private final AIFinancialAssistantService assistantService;
    private final SecurityUtils securityUtils;

    @PostMapping("/chat")
    @Operation(summary = "Ask the AI assistant a question about your finances")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(assistantService.chat(user, request.getMessage()));
    }

    @GetMapping("/history")
    @Operation(summary = "Get the current session's conversation history")
    public ResponseEntity<ConversationHistoryResponse> getHistory() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(assistantService.getHistory(user));
    }

    @DeleteMapping("/history")
    @Operation(summary = "Clear the current session's conversation history")
    public ResponseEntity<Void> clearHistory() {
        User user = securityUtils.getCurrentUser();
        assistantService.clearHistory(user);
        return ResponseEntity.noContent().build();
    }
}
