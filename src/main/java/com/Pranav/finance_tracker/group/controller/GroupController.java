package com.Pranav.finance_tracker.group.controller;

import com.Pranav.finance_tracker.group.dto.AddMemberRequest;
import com.Pranav.finance_tracker.group.dto.CreateGroupRequest;
import com.Pranav.finance_tracker.group.dto.GroupResponse;
import com.Pranav.finance_tracker.group.service.GroupInvitationService;
import com.Pranav.finance_tracker.group.service.GroupService;
import com.Pranav.finance_tracker.payment.service.GroupPaymentService;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Manage expense groups and members")
public class GroupController {

    private final GroupService groupService;
    private final GroupInvitationService invitationService;
    private final GroupPaymentService groupPaymentService;
    private final com.Pranav.finance_tracker.auth.security.SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create a new expense group")
    public ResponseEntity<GroupResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(groupService.createGroup(request, currentUser));
    }

    @PostMapping("/{groupId}/members")
    @Operation(summary = "Add a member to a group")
    public ResponseEntity<Void> addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMemberRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (request.getUserId() != null) {
            invitationService.inviteUserById(groupId, request.getUserId(), currentUser);
        } else if (request.getIdentifier() != null) {
            invitationService.inviteUser(groupId, request.getIdentifier(), currentUser);
        }
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
    @Operation(summary = "List all groups for the current user")
    public ResponseEntity<List<GroupResponse>> getMyGroups() {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(groupService.getMyGroups(currentUser));
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
    @Operation(summary = "Settle debt in a group")
    public ResponseEntity<String> settleDebt(
            @PathVariable UUID groupId,
            @Valid @RequestBody com.Pranav.finance_tracker.group.dto.CreatePaymentRequest request) {
        request.setGroupId(groupId);
        String msg = groupPaymentService.createPayment(request);
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/invitations")
    public ResponseEntity<List<java.util.Map<String, Object>>> getMyInvitations() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(invitationService.getMyInvitations(user).stream()
                .map(i -> java.util.Map.of(
                        "id", (Object) i.getId(),
                        "groupId", (Object) i.getGroup().getId(),
                        "groupName", (Object) i.getGroup().getName(),
                        "invitedBy", (Object) i.getInvitedBy().getName(),
                        "date", (Object) i.getCreatedAt().toString()
                ))
                .toList());
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(@PathVariable UUID invitationId) {
        User user = securityUtils.getCurrentUser();
        invitationService.acceptInvitation(invitationId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<Void> rejectInvitation(@PathVariable UUID invitationId) {
        User user = securityUtils.getCurrentUser();
        invitationService.rejectInvitation(invitationId, user);
        return ResponseEntity.ok().build();
    }
}
