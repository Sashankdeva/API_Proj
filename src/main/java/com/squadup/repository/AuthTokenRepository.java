package com.squadup.repository;

import com.squadup.entity.AuthToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<AuthToken> findByToken(String token);

    @Modifying
    @Query("delete from AuthToken t where t.token = :token")
    int deleteByToken(@Param("token") String token);

    @Modifying
    @Query("delete from AuthToken t where t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from AuthToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
