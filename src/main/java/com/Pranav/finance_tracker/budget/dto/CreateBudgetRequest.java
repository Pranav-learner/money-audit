package com.Pranav.finance_tracker.budget.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateBudgetRequest {

    @NotNull
    private UUID categoryId;

    @NotNull(message = "Limit amount is required")
    @jakarta.validation.constraints.Positive(message = "Limit amount must be positive")
    @jakarta.validation.constraints.DecimalMin(value = "0.01", message = "Limit amount must be at least 0.01")
    private BigDecimal limitAmount;

    @jakarta.validation.constraints.Min(value = 1, message = "Month must be between 1 and 12")
    @jakarta.validation.constraints.Max(value = 12, message = "Month must be between 1 and 12")
    private int month;

    @jakarta.validation.constraints.Min(value = 2000, message = "Year must be valid")
    private int year;
}

