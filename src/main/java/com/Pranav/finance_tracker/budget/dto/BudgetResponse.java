package com.Pranav.finance_tracker.budget.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(UUID categoryId, BigDecimal limitAmount, int month, int year) {
}
