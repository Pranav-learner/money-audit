package com.Pranav.finance_tracker.friend.dto;

import com.Pranav.finance_tracker.friend.enums.FriendshipStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {
    private UUID friendshipId;
    private UUID userId;
    private String userName;
    private String email;
    private FriendshipStatus status;
    private LocalDateTime since;
}
