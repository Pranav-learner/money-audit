package com.Pranav.finance_tracker.budget.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateBudgetRequest {

    @NotNull
    private UUID categoryId;

    @NotNull
    private BigDecimal limitAmount;

    @NotNull
    private int month;

    @NotNull
    private int year;
}

