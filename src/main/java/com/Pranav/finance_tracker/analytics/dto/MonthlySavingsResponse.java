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
public class MonthlySavingsResponse {
    private int month;
    private int year;
    private BigDecimal totalSaved;
}
