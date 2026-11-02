package com.Pranav.finance_tracker.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateExpenseRequest {
    @NotBlank
    private String title;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @jakarta.validation.constraints.DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @NotNull
    private LocalDate expenseDate;

    private String description;

    @NotNull
    private UUID categoryId;
}
