package com.squadup.repository;

import com.squadup.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ── 1. Derived query methods (query generated from the method name) ──
    /** Usernames are unique regardless of case, so "Admin" cannot imitate "admin". */
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    /** Lower-cased usernames that more than one account has, e.g. "Admin" and "admin" (old data only). */
    @Query("select lower(u.username) from User u group by lower(u.username) having count(u) > 1")
    List<String> findUsernamesDifferingOnlyByCase();

    /** Locks the row until the transaction ends (SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmail(String email);

    /** Derived query + pagination/sorting through {@link Pageable}. */
    Page<User> findByUsernameContainingIgnoreCase(String fragment, Pageable pageable);

    // ── 2. Named query: resolved from @NamedQuery "User.findByLogin" on the entity ──
    Optional<User> findByLogin(@Param("login") String login);

    // ── 3. @Query (JPQL) with a dynamic Sort supplied by the caller ──
    @Query("select u from User u where lower(u.favoriteGame) = lower(:game) or :game member of u.gameTags")
    List<User> findPlayersOfGame(@Param("game") String game, Pageable pageable);
}
