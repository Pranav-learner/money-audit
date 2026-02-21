package com.Pranav.finance_tracker.friend.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.email.service.EmailService;
import com.Pranav.finance_tracker.friend.dto.FriendResponse;
import com.Pranav.finance_tracker.friend.dto.UserSearchResponse;
import com.Pranav.finance_tracker.friend.entity.Friendship;
import com.Pranav.finance_tracker.friend.enums.FriendshipStatus;
import com.Pranav.finance_tracker.friend.repository.FriendshipRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final EmailService emailService;

    // ── Search users by name ──

    public List<UserSearchResponse> searchUsers(String query) {
        User currentUser = securityUtils.getCurrentUser();

        return userRepository.findByNameContainingIgnoreCase(query).stream()
                .filter(u -> !u.getId().equals(currentUser.getId())) // exclude self
                .map(u -> {
                    String relationshipStatus = resolveRelationshipStatus(currentUser, u);
                    return UserSearchResponse.builder()
                            .userId(u.getId())
                            .name(u.getName())
                            .email(u.getEmail())
                            .phone(u.getPhone())
                            .relationshipStatus(relationshipStatus)
                            .build();
                })
                .toList();
    }

    // ── Compute relationship status from current user's perspective ──

    private String resolveRelationshipStatus(User currentUser, User other) {
        return friendshipRepository.findRelationshipBetween(currentUser, other)
                .map(f -> {
                    return switch (f.getStatus()) {
                        case ACCEPTED -> "FRIEND";
                        case PENDING -> {
                            // PENDING_SENT = current user is the sender
                            // PENDING_RECEIVED = other user sent to current user
                            if (f.getSender().getId().equals(currentUser.getId())) {
                                yield "PENDING_SENT";
                            } else {
                                yield "PENDING_RECEIVED";
                            }
                        }
                        case REJECTED -> "NONE"; // treat rejected as no relationship
                    };
                })
                .orElse("NONE");
    }

    // ── Send friend request by phone number ──

    @Transactional
    public String sendRequest(String receiverPhone) {
        User sender = securityUtils.getCurrentUser();

        User receiver = userRepository.findByPhone(receiverPhone)
                .orElseThrow(() -> new RuntimeException("No user found with phone: " + receiverPhone));

        if (sender.getId().equals(receiver.getId())) {
            throw new RuntimeException("Cannot send friend request to yourself");
        }

        // Check if any relationship already exists (in either direction)
        if (friendshipRepository.existsBetweenUsers(sender, receiver)) {
            throw new RuntimeException("A friend request already exists between you and this user");
        }

        Friendship friendship = Friendship.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendshipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        friendshipRepository.save(friendship);

        // Send Notification
        String subject = "New Friend Request on Finance Tracker";
        String body = String.format("Hello %s,\n\n%s has sent you a friend request. Log in to accept it!",
                receiver.getName(), sender.getName());
        emailService.sendEmail(receiver, subject, body);

        return "Friend request sent to " + receiver.getName();
    }

    // ── Accept friend request ──

    @Transactional
    public String acceptRequest(UUID friendshipId) {
        User currentUser = securityUtils.getCurrentUser();

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendship.getReceiver().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the receiver can accept a friend request");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("This request is no longer pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        return "Friend request from " + friendship.getSender().getName() + " accepted";
    }

    // ── Reject friend request ──

    @Transactional
    public String rejectRequest(UUID friendshipId) {
        User currentUser = securityUtils.getCurrentUser();

        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendship.getReceiver().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the receiver can reject a friend request");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("This request is no longer pending");
        }

        friendship.setStatus(FriendshipStatus.REJECTED);
        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        return "Friend request from " + friendship.getSender().getName() + " rejected";
    }

    // ── List accepted friends ──

    public List<FriendResponse> getFriends() {
        User currentUser = securityUtils.getCurrentUser();

        return friendshipRepository.findAcceptedFriendships(currentUser).stream()
                .map(f -> {
                    User friend = f.getSender().getId().equals(currentUser.getId())
                            ? f.getReceiver()
                            : f.getSender();

                    return FriendResponse.builder()
                            .friendshipId(f.getId())
                            .userId(friend.getId())
                            .userName(friend.getName())
                            .email(friend.getEmail())
                            .status(f.getStatus())
                            .since(f.getUpdatedAt() != null ? f.getUpdatedAt() : f.getCreatedAt())
                            .build();
                })
                .toList();
    }

    // ── List pending incoming requests ──

    public List<FriendResponse> getPendingRequests() {
        User currentUser = securityUtils.getCurrentUser();

        return friendshipRepository
                .findByReceiverAndStatus(currentUser, FriendshipStatus.PENDING).stream()
                .map(f -> FriendResponse.builder()
                        .friendshipId(f.getId())
                        .userId(f.getSender().getId())
                        .userName(f.getSender().getName())
                        .email(f.getSender().getEmail())
                        .status(f.getStatus())
                        .since(f.getCreatedAt())
                        .build())
                .toList();
    }
}
