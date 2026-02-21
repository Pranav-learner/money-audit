package com.Pranav.finance_tracker.expense.dto;

import com.Pranav.finance_tracker.group.enums.SplitType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateDirectExpenseRequest {
    private String otherUserPhone;
    private String title;
    private BigDecimal totalAmount;
    private LocalDate expenseDate;
    private SplitType splitType;
    private BigDecimal myShare;        // for UNEQUAL split
    private BigDecimal otherShare;     // for UNEQUAL split
    private BigDecimal myPercentage;   // for PERCENTAGE split
    private BigDecimal otherPercentage; // for PERCENTAGE split
}
