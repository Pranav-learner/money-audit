package com.Pranav.finance_tracker.group.service;

import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupInvitation;
import com.Pranav.finance_tracker.group.repository.GroupInvitationRepository;
import com.Pranav.finance_tracker.group.repository.GroupRepository;
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
public class GroupInvitationService {

    private final GroupInvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    @Transactional
    public void inviteUser(UUID groupId, String identifier, User invitedBy) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User invitedUser = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new RuntimeException("User not found with: " + identifier));

        if (invitationRepository.findByGroupAndInvitedUserAndStatus(group, invitedUser, GroupInvitation.InvitationStatus.PENDING).isPresent()) {
            throw new RuntimeException("Invitation already pending for this user");
        }

        GroupInvitation invitation = GroupInvitation.builder()
                .group(group)
                .invitedUser(invitedUser)
                .invitedBy(invitedBy)
                .status(GroupInvitation.InvitationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        invitationRepository.save(invitation);
    }

    @Transactional
    public void inviteUserById(UUID groupId, UUID userId, User invitedBy) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User invitedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (invitationRepository.findByGroupAndInvitedUserAndStatus(group, invitedUser, GroupInvitation.InvitationStatus.PENDING).isPresent()) {
            throw new RuntimeException("Invitation already pending for this user");
        }

        GroupInvitation invitation = GroupInvitation.builder()
                .group(group)
                .invitedUser(invitedUser)
                .invitedBy(invitedBy)
                .status(GroupInvitation.InvitationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        invitationRepository.save(invitation);
    }

    @Transactional
    public void acceptInvitation(UUID invitationId, User user) {
        GroupInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.getInvitedUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        invitation.setStatus(GroupInvitation.InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        groupService.addMember(invitation.getGroup().getId(), user.getId());
    }

    @Transactional
    public void rejectInvitation(UUID invitationId, User user) {
        GroupInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.getInvitedUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        invitation.setStatus(GroupInvitation.InvitationStatus.REJECTED);
        invitationRepository.save(invitation);
    }

    public List<GroupInvitation> getMyInvitations(User user) {
        return invitationRepository.findByInvitedUserAndStatus(user, GroupInvitation.InvitationStatus.PENDING);
    }
}
