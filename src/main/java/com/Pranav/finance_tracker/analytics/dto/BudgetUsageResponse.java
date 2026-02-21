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
public class BudgetUsageResponse {
    private String category;
    private BigDecimal budget;
    private BigDecimal spent;
    private BigDecimal remaining;
    private int percentageUsed;
    private String status; // "NORMAL" or "OVER_BUDGET"
}
