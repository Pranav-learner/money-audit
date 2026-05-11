package com.Pranav.finance_tracker.group.controller;

import com.Pranav.finance_tracker.group.dto.AddMemberRequest;
import com.Pranav.finance_tracker.group.dto.CreateGroupRequest;
import com.Pranav.finance_tracker.group.dto.GroupResponse;
import com.Pranav.finance_tracker.group.service.GroupService;
import com.Pranav.finance_tracker.payment.service.GroupPaymentService;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupPaymentService groupPaymentService;

    @PostMapping
    public GroupResponse createGroup(
            @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return groupService.createGroup(request, currentUser);
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable UUID groupId,
            @RequestBody AddMemberRequest request) {
        groupService.addMember(groupId, request.getUserId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<String> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {
        groupService.removeMember(groupId, userId);
        return ResponseEntity.ok("Member removed successfully");
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<String> deleteGroup(
            @PathVariable UUID groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.ok("Group deleted successfully");
    }

    @GetMapping
    public List<GroupResponse> getMyGroups(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return groupService.getMyGroups(currentUser);
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(@PathVariable UUID groupId) {
        return groupService.getGroupById(groupId);
    }

    @GetMapping("/{groupId}/members")
    public List<java.util.Map<String, Object>> getGroupMembers(@PathVariable UUID groupId) {
        return groupService.getGroupMembers(groupId);
    }

    @PostMapping("/{groupId}/settle")
    public ResponseEntity<String> settleDebt(
            @PathVariable UUID groupId,
            @RequestBody com.Pranav.finance_tracker.group.dto.CreatePaymentRequest request) {
        request.setGroupId(groupId);
        String msg = groupPaymentService.createPayment(request);
        return ResponseEntity.ok(msg);
    }
}
