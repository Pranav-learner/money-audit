package com.Pranav.finance_tracker.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    private UUID groupId;

    @NotNull
    private UUID toUserId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String note;
}
