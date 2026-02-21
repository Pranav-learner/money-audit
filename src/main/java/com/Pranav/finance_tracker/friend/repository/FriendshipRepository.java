package com.Pranav.finance_tracker.friend.repository;

import com.Pranav.finance_tracker.friend.entity.Friendship;
import com.Pranav.finance_tracker.friend.enums.FriendshipStatus;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    Optional<Friendship> findBySenderAndReceiver(User sender, User receiver);

    List<Friendship> findByReceiverAndStatus(User receiver, FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.sender = :user OR f.receiver = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("user") User user);

    // Checks both directions — A→B and B→A
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
            "WHERE (f.sender = :user1 AND f.receiver = :user2) " +
            "OR (f.sender = :user2 AND f.receiver = :user1)")
    boolean existsBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    // Returns the Friendship record in either direction, so service can inspect sender/receiver
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.sender = :user1 AND f.receiver = :user2) " +
            "OR (f.sender = :user2 AND f.receiver = :user1)")
    Optional<Friendship> findRelationshipBetween(@Param("user1") User user1, @Param("user2") User user2);
}
