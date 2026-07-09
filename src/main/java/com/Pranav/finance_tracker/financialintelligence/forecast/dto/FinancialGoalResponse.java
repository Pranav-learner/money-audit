package com.Pranav.finance_tracker.financialintelligence.forecast.dto;

import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalStatus;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Client-facing view of a {@code FinancialGoal}, including derived progress. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialGoalResponse {

    private UUID id;
    private String title;
    private GoalType goalType;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private BigDecimal monthlyContributionRequired;
    private LocalDate projectedCompletionDate;
    private GoalStatus status;

    /** Progress toward the target as a percentage (0–100). */
    private int progressPercent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
