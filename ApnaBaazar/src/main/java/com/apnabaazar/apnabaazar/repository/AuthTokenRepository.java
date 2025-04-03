package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.token.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {
    Optional<AuthToken> findByToken(String token);
    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(Instant now);
}
