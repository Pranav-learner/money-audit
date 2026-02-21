package com.Pranav.finance_tracker.friend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateEditRequestDTO {
    private BigDecimal newAmount;
    private String note;
}
