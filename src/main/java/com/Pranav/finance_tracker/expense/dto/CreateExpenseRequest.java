package com.Pranav.finance_tracker.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateExpenseRequest {
    @NotBlank
    private String title;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private LocalDate expenseDate;

    private String description;

    @NotNull
    private UUID categoryId;
}
