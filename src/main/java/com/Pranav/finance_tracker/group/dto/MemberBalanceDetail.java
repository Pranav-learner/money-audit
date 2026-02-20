package com.Pranav.finance_tracker.group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class MemberBalanceDetail {

    private UUID userId;
    private String userName;
    private BigDecimal amount;
    private BalanceType type;

    public enum BalanceType {
        YOU_OWE,
        YOU_ARE_OWED,
        SETTLED
    }
}
