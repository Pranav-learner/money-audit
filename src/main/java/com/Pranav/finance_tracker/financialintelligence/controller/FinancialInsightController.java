package com.Pranav.finance_tracker.financialintelligence.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.dto.FinancialInsightResponse;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.service.FinancialInsightService;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for the AI Insights page. All endpoints operate on the authenticated user only.
 */
@RestController
@RequestMapping("/api/financial-insights")
@RequiredArgsConstructor
@Tag(name = "Financial Insights", description = "Personalized, rule-generated financial intelligence")
public class FinancialInsightController {

    private final FinancialInsightService insightService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List all active (non-dismissed, non-expired) insights")
    public ResponseEntity<List<FinancialInsightResponse>> getInsights() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(insightService.getActiveInsights(user));
    }

    @GetMapping("/unread")
    @Operation(summary = "List active insights the user has not yet viewed")
    public ResponseEntity<List<FinancialInsightResponse>> getUnreadInsights() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(insightService.getUnreadInsights(user));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark an insight as viewed")
    public ResponseEntity<FinancialInsightResponse> markAsRead(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(insightService.markAsRead(user, id));
    }

    @PutMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss an insight")
    public ResponseEntity<FinancialInsightResponse> dismiss(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(insightService.dismiss(user, id));
    }

    @GetMapping("/summary")
    @Operation(summary = "Header summary: totals, unread count, high-severity count and latest insight")
    public ResponseEntity<InsightSummaryResponse> getSummary() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(insightService.getSummary(user));
    }
}
