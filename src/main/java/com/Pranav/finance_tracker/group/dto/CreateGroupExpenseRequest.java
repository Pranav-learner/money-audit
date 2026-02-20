package com.Pranav.finance_tracker.group.dto;

import com.Pranav.finance_tracker.group.enums.SplitType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateGroupExpenseRequest {
    private UUID groupId;
    private String title;
    private BigDecimal totalAmount;
    private LocalDate ExpenseDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitType splitType;

    private List<SplitDetail> splits;

}
