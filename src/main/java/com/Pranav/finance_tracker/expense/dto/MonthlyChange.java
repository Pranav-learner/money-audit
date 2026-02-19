package com.Pranav.finance_tracker.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlyChange {
    private BigDecimal currentMonthTotal;
    private BigDecimal lastMonthTotal;
    private double percentageChange;
}
