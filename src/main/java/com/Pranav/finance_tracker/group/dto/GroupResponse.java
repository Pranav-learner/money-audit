package com.Pranav.finance_tracker.group.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class GroupResponse {

    private UUID id;
    private String name;
    private String createdBy;
    private LocalDateTime createdAt;
}

