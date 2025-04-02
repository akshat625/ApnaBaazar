package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.token.UserVerificationToken;
import com.apnabaazar.apnabaazar.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserVerificationTokenRepository extends JpaRepository<UserVerificationToken, UUID> {
    Optional<UserVerificationToken> findByToken(String token);
    void deleteByUser(User user);

    void deleteByExpiresAtBefore(Instant now);
}
