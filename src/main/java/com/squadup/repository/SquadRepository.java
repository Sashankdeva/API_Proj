package com.squadup.repository;

import com.squadup.entity.Squad;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Squad data access. Extends {@link SquadRepositoryCustom}, so Spring Data merges the
 * hand-written {@code SquadRepositoryCustomImpl} (Criteria API search) into this proxy.
 */
public interface SquadRepository extends JpaRepository<Squad, Long>, SquadRepositoryCustom {

    /** Derived query that navigates the many-to-many "members" association. */
    List<Squad> findByMembers_IdOrderByCreatedAtDesc(Long userId);

    List<Squad> findByCreatedBy_Id(Long userId);

    /** Squads the user has NOT joined yet (used by the home page "discover" list). */
    @Query("""
            select s from Squad s
            where not exists (select 1 from Squad s2 join s2.members m where s2 = s and m.id = :userId)
            order by s.createdAt desc""")
    List<Squad> findNotJoinedBy(@Param("userId") Long userId);

    /** Pagination on a derived query. */
    Page<Squad> findByGameIgnoreCase(String game, Pageable pageable);

    /** Custom sorting with @Query: sort by member count, which a plain property Sort cannot express. */
    @Query(value = "select s from Squad s order by size(s.members) desc, s.createdAt desc",
            countQuery = "select count(s) from Squad s")
    Page<Squad> findMostPopular(Pageable pageable);

    /** @Query combined with a caller-supplied Sort. */
    @Query("select s from Squad s where lower(s.language) = lower(:language)")
    List<Squad> findByLanguage(@Param("language") String language, Sort sort);

    /** Row lock so two players cannot both take the last free slot. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Squad s where s.id = :id")
    Optional<Squad> findByIdForUpdate(@Param("id") Long id);

    /** Native bulk delete on the join table (used when an account is deleted). */
    @Modifying
    @Query(value = "delete from squad_members where user_id = :userId", nativeQuery = true)
    int removeMemberFromAllSquads(@Param("userId") Long userId);
}
