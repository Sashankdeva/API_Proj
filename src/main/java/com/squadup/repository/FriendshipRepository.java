package com.squadup.repository;

import com.squadup.entity.Friendship;
import com.squadup.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Every friendship of a user in the given state, whichever side sent the request. */
    @Query("""
            select f from Friendship f
            join fetch f.requester join fetch f.recipient
            where (f.requester.id = :userId or f.recipient.id = :userId) and f.status = :status""")
    List<Friendship> findAllForUser(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    /** Incoming requests. Derived query; the entity graph loads the requester in the same select. */
    @EntityGraph(attributePaths = "requester")
    List<Friendship> findByRecipient_IdAndStatusOrderByCreatedAtDesc(Long recipientId, FriendshipStatus status);

    @Query("""
            select f from Friendship f
            where (f.requester.id = :a and f.recipient.id = :b)
               or (f.requester.id = :b and f.recipient.id = :a)""")
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    /** Update operation in Spring Data JPA: a bulk JPQL UPDATE. */
    @Modifying(clearAutomatically = true)
    @Query("update Friendship f set f.status = :status where f.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") FriendshipStatus status);

    @Modifying
    @Query("delete from Friendship f where f.requester.id = :userId or f.recipient.id = :userId")
    int deleteAllForUser(@Param("userId") Long userId);
}
