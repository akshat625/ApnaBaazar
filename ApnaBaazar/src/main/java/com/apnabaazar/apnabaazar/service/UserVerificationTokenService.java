package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.repository.UserVerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class UserVerificationTokenService {

    @Autowired
    private UserVerificationTokenRepository userVerificationTokenRepository;

    @Scheduled(fixedRate = 3600000)
    public void deleteExpiredTokens() {
        Instant now = Instant.now();
        userVerificationTokenRepository.deleteByExpiresAtBefore(now);
    }
}
