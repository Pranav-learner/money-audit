package com.Pranav.finance_tracker.financialintelligence.healthscore.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScorePointResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScoreResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.service.HealthScoreService;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for the Financial Health Score Engine. Operates on the authenticated user only.
 */
@RestController
@RequestMapping("/api/health-score")
@RequiredArgsConstructor
@Tag(name = "Financial Health Score", description = "Explainable financial health score, breakdown and history")
public class HealthScoreController {

    private final HealthScoreService healthScoreService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Current health score with component breakdown and change since last snapshot")
    public ResponseEntity<HealthScoreResponse> getCurrentScore() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(healthScoreService.getCurrentScore(user));
    }

    @GetMapping("/history")
    @Operation(summary = "Recent health-score history (trend)")
    public ResponseEntity<List<HealthScorePointResponse>> getHistory(
            @RequestParam(defaultValue = "30") int limit) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(healthScoreService.getHistory(user, limit));
    }
}
