package com.Pranav.finance_tracker.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateDirectPaymentRequest {
    private String toUserPhone;
    private BigDecimal amount;
    private String note;
    private String requestId;
}
