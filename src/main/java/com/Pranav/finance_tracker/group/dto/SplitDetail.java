package com.Pranav.finance_tracker.group.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SplitDetail {

    private UUID userId;

    // Used for UNEQUAL
    private BigDecimal amount;

    // Used for PERCENTAGE
    private BigDecimal percentage;
}
