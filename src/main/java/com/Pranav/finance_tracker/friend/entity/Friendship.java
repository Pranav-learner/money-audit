package com.Pranav.finance_tracker.friend.entity;

import com.Pranav.finance_tracker.friend.enums.FriendshipStatus;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "friendships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sender_id", "receiver_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
