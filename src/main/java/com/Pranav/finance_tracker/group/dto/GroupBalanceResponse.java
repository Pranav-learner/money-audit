package com.Pranav.finance_tracker.group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class GroupBalanceResponse {

    private UUID userId;
    private String userName;
    private BigDecimal netBalance;
}
