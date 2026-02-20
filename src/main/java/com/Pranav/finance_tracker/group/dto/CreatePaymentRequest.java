package com.Pranav.finance_tracker.group.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreatePaymentRequest {

    private UUID groupId;
    private UUID toUserId;
    private BigDecimal amount;
    private String note;
    private String requestId;
}
