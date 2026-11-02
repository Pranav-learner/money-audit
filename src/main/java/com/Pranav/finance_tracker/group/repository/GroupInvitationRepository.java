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
    @org.springframework.data.jpa.repository.Query("SELECT gi FROM GroupInvitation gi JOIN FETCH gi.group JOIN FETCH gi.invitedBy WHERE gi.invitedUser = :user AND gi.status = :status")
    List<GroupInvitation> findByInvitedUserAndStatus(@org.springframework.data.repository.query.Param("user") User user, @org.springframework.data.repository.query.Param("status") GroupInvitation.InvitationStatus status);
    Optional<GroupInvitation> findByGroupAndInvitedUserAndStatus(Group group, User user, GroupInvitation.InvitationStatus status);
}
