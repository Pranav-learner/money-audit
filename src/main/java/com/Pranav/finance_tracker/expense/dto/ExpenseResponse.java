package com.Pranav.finance_tracker.expense.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
public class ExpenseResponse {

    private UUID id;
    private String title;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
    private String categoryName;
}

