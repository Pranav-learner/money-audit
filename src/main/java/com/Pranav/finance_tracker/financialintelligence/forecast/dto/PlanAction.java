package com.Pranav.finance_tracker.financialintelligence.forecast.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single actionable line in a goal plan, e.g. "Reduce food spending" → ₹2,100/month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanAction {

    /** What to do, e.g. "Reduce food spending" or "Increase monthly savings". */
    private String label;

    /** The monthly rupee impact of this action. */
    private BigDecimal monthlyAmount;
}
