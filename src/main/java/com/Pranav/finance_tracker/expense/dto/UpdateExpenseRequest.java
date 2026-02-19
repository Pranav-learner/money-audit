package com.Pranav.finance_tracker.expense.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateExpenseRequest {
    private String title;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private UUID categoryId;
}
