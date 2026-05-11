package com.Pranav.finance_tracker.friend.controller;

import com.Pranav.finance_tracker.friend.dto.FriendRequestDTO;
import com.Pranav.finance_tracker.friend.dto.FriendResponse;
import com.Pranav.finance_tracker.friend.dto.UserSearchResponse;
import com.Pranav.finance_tracker.friend.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipService friendshipService;

    @GetMapping("/api/users/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String query) {
        return ResponseEntity.ok(friendshipService.searchUsers(query));
    }

    @PostMapping("/api/friends/requests")
    public ResponseEntity<String> sendRequest(
            @RequestBody FriendRequestDTO request) {
        return ResponseEntity.ok(friendshipService.sendRequest(request.getPhoneUrlEmail()));
    }

    @PatchMapping("/api/friends/requests/{id}")
    public ResponseEntity<String> updateRequestStatus(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(friendshipService.updateRequestStatus(id, body.get("status")));
    }

    @PutMapping("/api/friends/{id}/accept")
    public ResponseEntity<String> acceptRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(friendshipService.acceptRequest(id));
    }

    @PutMapping("/api/friends/{id}/reject")
    public ResponseEntity<String> rejectRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(friendshipService.rejectRequest(id));
    }

    @GetMapping("/api/friends")
    public ResponseEntity<List<FriendResponse>> getFriends() {
        return ResponseEntity.ok(friendshipService.getFriends());
    }

    @GetMapping("/api/friends/requests")
    public ResponseEntity<List<FriendResponse>> getPendingRequests() {
        return ResponseEntity.ok(friendshipService.getPendingRequests());
    }
}
