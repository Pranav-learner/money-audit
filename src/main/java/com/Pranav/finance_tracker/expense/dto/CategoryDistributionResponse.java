package com.Pranav.finance_tracker.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategoryDistributionResponse {

    private String name;
    private BigDecimal value;
    private String color;
    public CategoryDistributionResponse(String name, BigDecimal value) {
        this.name = name;
        this.value = value;
    }
}

