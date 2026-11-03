package com.Pranav.finance_tracker.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private UUID id;
    private String title;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String date;
    private String description;
    private UUID categoryId;
    private String category;
    private String categoryName;
    private String icon;
}

