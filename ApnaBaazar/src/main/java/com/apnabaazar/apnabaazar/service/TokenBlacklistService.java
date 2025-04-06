package com.apnabaazar.apnabaazar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_ACCESS_PREFIX = "bl_access_token:";
    private static final String BLACKLIST_RESET_PREFIX = "bl_reset_token:";


    public void blacklistAccessToken(String token, long timeToExpire) {
        String key = BLACKLIST_ACCESS_PREFIX + token;
        redisTemplate.opsForValue().set(key,"blacklisted",timeToExpire, TimeUnit.MILLISECONDS);
    }

    public void blacklistResetToken(String token, long timeToExpire) {
        String key = BLACKLIST_RESET_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", timeToExpire, TimeUnit.MILLISECONDS);
    }

    public boolean isAccessTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_ACCESS_PREFIX + token));
    }

    public String getStoredToken(String key){
        return redisTemplate.opsForValue().get(key);
    }
}
