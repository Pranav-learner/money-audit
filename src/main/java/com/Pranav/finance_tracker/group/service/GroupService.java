package com.Pranav.finance_tracker.group.service;

import com.Pranav.finance_tracker.group.dto.CreateGroupRequest;
import com.Pranav.finance_tracker.group.dto.GroupResponse;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupMember;
import com.Pranav.finance_tracker.group.repository.GroupMemberRepository;
import com.Pranav.finance_tracker.group.repository.GroupRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import com.Pranav.finance_tracker.expense.service.GroupBalanceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupBalanceService groupBalanceService;

    public GroupResponse createGroup(CreateGroupRequest request, User currentUser){

        Group group = Group.builder()
                .name(request.getName())
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        groupRepository.save(group);

        GroupMember creatorMember = GroupMember.builder()
                .group(group)
                .user(currentUser)
                .joinedAt(LocalDateTime.now())
                .build();

        groupMemberRepository.save(creatorMember);

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .createdBy(currentUser.getName())
                .createdAt(group.getCreatedAt())
                .build();
    }

    public void addMember(UUID groupId, UUID userId){

        Group group = groupRepository.findById(groupId)
                .orElseThrow(()-> new RuntimeException("Group not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not Found"));

        if(groupMemberRepository.existsByGroupAndUser(group,user)){
            throw new RuntimeException("User is already a member of the group");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();

        groupMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            throw new RuntimeException("User is not a member of this group");
        }

        // 🔒 Lock: cannot remove member if group has unsettled balances
        if (groupBalanceService.hasUnsettledBalances(groupId)) {
            throw new RuntimeException(
                    "Cannot remove member: group has unsettled balances. " +
                    "All debts must be settled first.");
        }

        groupMemberRepository.deleteByGroupAndUser(group, user);
    }

    @Transactional
    public void deleteGroup(UUID groupId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // 🔒 Lock: cannot delete group if balances are not zero
        if (groupBalanceService.hasUnsettledBalances(groupId)) {
            throw new RuntimeException(
                    "Cannot delete group: unsettled balances exist. " +
                    "All debts must be settled first.");
        }

        groupMemberRepository.deleteByGroup(group);
        groupRepository.delete(group);
    }

    public List<GroupResponse> getMyGroups(User currentUser) {

        List<GroupMember> memberships = groupMemberRepository.findByUser(currentUser);

        return memberships.stream()
                .map(m -> {
                    Group g = m.getGroup();
                    return GroupResponse.builder()
                            .id(g.getId())
                            .name(g.getName())
                            .createdBy(g.getCreatedBy().getName())
                            .createdAt(g.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
