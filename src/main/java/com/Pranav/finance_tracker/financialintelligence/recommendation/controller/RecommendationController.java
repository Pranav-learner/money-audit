package com.Pranav.finance_tracker.financialintelligence.recommendation.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.FinancialRecommendationResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationService;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for the Personalized Financial Recommendation Engine. All endpoints operate on the
 * authenticated user only.
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalized, rule-generated financial recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List active recommendations")
    public ResponseEntity<List<FinancialRecommendationResponse>> getRecommendations() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(recommendationService.getActiveRecommendations(user));
    }

    @GetMapping("/top")
    @Operation(summary = "List the top-priority recommendations")
    public ResponseEntity<List<FinancialRecommendationResponse>> getTopRecommendations() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(recommendationService.getTopRecommendations(user));
    }

    @GetMapping("/history")
    @Operation(summary = "Full recommendation history (all statuses)")
    public ResponseEntity<List<FinancialRecommendationResponse>> getHistory() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(recommendationService.getHistory(user));
    }

    @PutMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss a recommendation")
    public ResponseEntity<FinancialRecommendationResponse> dismiss(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(recommendationService.dismiss(user, id));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Mark a recommendation as completed")
    public ResponseEntity<FinancialRecommendationResponse> complete(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(recommendationService.complete(user, id));
    }

    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary: totals, potential savings, top and recently-completed recommendations")
    public ResponseEntity<RecommendationSummaryResponse> getSummary() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(recommendationService.getSummary(user));
    }
}
