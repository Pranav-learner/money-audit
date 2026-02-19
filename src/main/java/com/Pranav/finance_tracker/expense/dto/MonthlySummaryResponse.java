package com.Pranav.finance_tracker.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlySummaryResponse {

    private BigDecimal totalAmount;
    private Long totalCount;
}

