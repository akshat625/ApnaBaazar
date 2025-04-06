package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.repository.AuthTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class AuthTokenService {

    @Autowired
    private AuthTokenRepository authTokenRepository;


    @Scheduled(fixedRate = 3600000)
    public void deleteExpiredTokens() {
        Instant now = Instant.now();
        authTokenRepository.deleteByExpiresAtBefore(now);
    }


}
