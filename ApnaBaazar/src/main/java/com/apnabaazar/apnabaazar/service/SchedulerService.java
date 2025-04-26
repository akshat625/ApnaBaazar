package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.AuthTokenRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SchedulerService {
    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Scheduled(fixedRate = 60*60000)
    public void deleteExpiredTokens() {
        Instant now = Instant.now();
        log.info("Starting scheduled task to delete expired tokens. Current time: {}", now);

        int deletedCount = authTokenRepository.deleteByExpiresAtBefore(now);

        if (deletedCount > 0) {
            log.info("Successfully deleted {} expired tokens.", deletedCount);
        } else {
            log.info("No expired tokens to delete at this time.");
        }
    }


    @Scheduled(fixedRate = 60 * 60 * 1000) // Run every hour
    public void unlockLockedAccounts() {
        log.info("Starting scheduled task to unlock accounts locked for more than 3 hours");
        LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);

        List<User> lockedUsers = userRepository.findByIsLockedTrue();
        int unlockCount = 0;

        for (User user : lockedUsers) {
            // Check if the user's last update was at least 3 hours ago
            // This assumes updatedAt is set when the account is locked
            if (user.getUpdatedAt() != null && user.getUpdatedAt().isBefore(threeHoursAgo)) {
                user.setLocked(false);
                user.setInvalidAttemptCount(0);
                userRepository.save(user);
                unlockCount++;

                log.info("Automatically unlocked account for user: {}", user.getEmail());
            }
        }

        if (unlockCount > 0) {
            log.info("Unlocked {} account(s) during scheduled unlock task", unlockCount);
        } else {
            log.debug("No accounts eligible for automatic unlock at this time");
        }
    }

}
