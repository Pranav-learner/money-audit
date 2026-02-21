package com.Pranav.finance_tracker.friend.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private String relationshipStatus; // NONE | PENDING_SENT | PENDING_RECEIVED | FRIEND
}
