package com.Pranav.finance_tracker.financialintelligence.forecast.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.FinancialGoalResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalForecastResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalPlanResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.GoalRequest;
import com.Pranav.finance_tracker.financialintelligence.forecast.service.GoalService;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for financial goals and their plans. Operates on the authenticated user only.
 */
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Financial goals, plans and completion forecasts")
public class GoalController {

    private final GoalService goalService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List active goals")
    public ResponseEntity<List<FinancialGoalResponse>> getGoals() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(goalService.getActiveGoals(user));
    }

    @PostMapping
    @Operation(summary = "Create a goal")
    public ResponseEntity<FinancialGoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(goalService.createGoal(user, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a goal")
    public ResponseEntity<FinancialGoalResponse> updateGoal(@PathVariable UUID id, @Valid @RequestBody GoalRequest request) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(goalService.updateGoal(user, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a goal")
    public ResponseEntity<Void> deleteGoal(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();
        goalService.deleteGoal(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/plan")
    @Operation(summary = "Get the complete financial plan for a goal")
    public ResponseEntity<GoalPlanResponse> getPlan(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(goalService.getPlan(user, id));
    }

    @GetMapping("/{id}/forecast")
    @Operation(summary = "Get completion probability, projected date, required savings and recommendations")
    public ResponseEntity<GoalForecastResponse> getGoalForecast(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(goalService.getGoalForecast(user, id));
    }
}
