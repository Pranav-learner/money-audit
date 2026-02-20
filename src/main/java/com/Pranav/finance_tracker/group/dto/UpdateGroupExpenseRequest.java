package com.Pranav.finance_tracker.group.dto;

import com.Pranav.finance_tracker.group.enums.SplitType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateGroupExpenseRequest {
    private String title;
    private BigDecimal totalAmount;
    private LocalDate expenseDate;
    private SplitType splitType;
    private List<SplitDetail> splits;
}
