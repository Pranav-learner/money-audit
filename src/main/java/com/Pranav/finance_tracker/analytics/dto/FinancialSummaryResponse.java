package com.Pranav.finance_tracker.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryResponse {
    private BigDecimal totalIncomeSaved; // Total savings in that month
    private BigDecimal totalSpent;       // Total personal expenses in that month
    private BigDecimal netSavings;      // IncomeSaved - TotalSpent
    private String topCategory;         // Category with highest spending
}
