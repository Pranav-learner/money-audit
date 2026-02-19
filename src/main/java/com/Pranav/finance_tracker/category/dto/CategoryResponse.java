package com.Pranav.finance_tracker.category.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private boolean isSystem;
}
