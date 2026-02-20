package com.Pranav.finance_tracker.group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class BalanceSummaryResponse {

    private UUID groupId;
    private BigDecimal netBalance;
    private List<MemberBalanceDetail> balances;
}
