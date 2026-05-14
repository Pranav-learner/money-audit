package com.Pranav.finance_tracker.group.repository;

import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupInvitation;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, UUID> {
    List<GroupInvitation> findByInvitedUserAndStatus(User user, GroupInvitation.InvitationStatus status);
    Optional<GroupInvitation> findByGroupAndInvitedUserAndStatus(Group group, User user, GroupInvitation.InvitationStatus status);
}
