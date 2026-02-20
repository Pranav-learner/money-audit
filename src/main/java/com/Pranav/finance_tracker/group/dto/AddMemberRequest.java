package com.Pranav.finance_tracker.group.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AddMemberRequest {
    private UUID userId;
}

