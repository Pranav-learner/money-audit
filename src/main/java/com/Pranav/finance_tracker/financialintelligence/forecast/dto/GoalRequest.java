package com.Pranav.finance_tracker.financialintelligence.forecast.dto;

import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload for creating or updating a {@code FinancialGoal}. */
@Data
public class GoalRequest {

    @NotNull
    @Size(min = 1, max = 160)
    private String title;

    @NotNull
    private GoalType goalType;

    @NotNull
    @Positive
    private BigDecimal targetAmount;

    /** Amount already put aside toward the goal; defaults to zero when omitted. */
    private BigDecimal currentAmount;

    @NotNull
    @FutureOrPresent
    private LocalDate targetDate;
}
