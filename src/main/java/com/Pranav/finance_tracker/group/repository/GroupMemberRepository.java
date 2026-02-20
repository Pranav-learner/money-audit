package com.Pranav.finance_tracker.group.repository;

import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupMember;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    List<GroupMember> findByUser(User user);

    List<GroupMember> findByGroup(Group group);

    boolean existsByGroupAndUser(Group group, User user);

    List<GroupMember> findByGroupId(UUID groupId);

    void deleteByGroupAndUser(Group group, User user);

    void deleteByGroup(Group group);

}
