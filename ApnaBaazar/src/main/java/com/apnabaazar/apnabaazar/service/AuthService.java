package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.model.dto.*;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerDTO;
import com.apnabaazar.apnabaazar.model.token.AuthToken;
import com.apnabaazar.apnabaazar.enums.TokenType;
import com.apnabaazar.apnabaazar.model.users.*;
import com.apnabaazar.apnabaazar.repository.AuthTokenRepository;
import com.apnabaazar.apnabaazar.repository.RoleRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
//@Transactional
public class AuthService {

    private final TokenBlacklistService tokenBlacklistService;
    private final MessageSource messageSource;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthTokenRepository authTokenRepository;


    public String customerSignup(CustomerDTO input) {
        Locale locale = LocaleContextHolder.getLocale();
        log.info("Starting customer signup process for email: {}", input.getEmail());

        if (!input.getPassword().equals(input.getConfirmPassword())) {
            log.warn("Password mismatch for email: {}", input.getEmail());
            throw new PasswordMismatchException(messageSource.getMessage("password.mismatch", null, locale));
        }

        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            log.warn("Attempt to register with already used email: {}", input.getEmail());
            throw new EmailAlreadyInUseException(messageSource.getMessage("email.in.use", null, locale));
        }

        Customer customer = new Customer();
        customer.setEmail(input.getEmail());
        customer.setPassword(passwordEncoder.encode(input.getPassword()));
        customer.setPasswordUpdateDate(LocalDateTime.now());
        customer.setFirstName(input.getFirstName());
        customer.setLastName(input.getLastName());
        customer.setContact(input.getContact());

        Role role = roleRepository.findByAuthority("ROLE_CUSTOMER")
                .orElseThrow(() -> {
                    log.error("Customer role not found in database.");
                    return new RoleNotFoundException(messageSource.getMessage("role.not.found", null, locale));
                });

        customer.addRole(role);

        if (input.getMiddleName() != null && !input.getMiddleName().isEmpty()) {
            customer.setMiddleName(input.getMiddleName());
        }

        userRepository.save(customer);
        log.info("Customer saved successfully with email: {}", customer.getEmail());

        String activationToken = jwtService.generateActivationToken(customer.getEmail());
        AuthToken authToken = new AuthToken(
                activationToken,
                customer.getEmail(),
                Instant.now().plusMillis(jwtService.getActivationTokenExpirationTime())
        );
        authTokenRepository.save(authToken);
        log.info("Activation token generated and saved for email: {}", customer.getEmail());

        try {
            emailService.sendVerificationEmail(customer.getEmail(), "Account Verification", activationToken);
            log.info("Verification email sent to: {}", customer.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", customer.getEmail(), e.getMessage());
            throw new EmailSendingException(messageSource.getMessage("email.sending.failed", null, locale), e);
        }

        log.info("Customer signup completed successfully for email: {}", customer.getEmail());
        return messageSource.getMessage("user.registered.success", null, locale);
    }


    public String verifyUser(String token) {
        log.info("Verifying user with token");

        String emailId = jwtService.extractUsername(token);
        AuthToken verificationToken = authTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Verification token not found or already used for email: {}", emailId);
                    return new VerificationTokenNotFoundException(messageSource.getMessage("token.not.found.or.used", null, LocaleContextHolder.getLocale()));
                });

        if (jwtService.isTokenExpired(token)) {
            log.info("Token expired for email: {}. Generating new token.", emailId);
            authTokenRepository.delete(verificationToken);
            String newToken = jwtService.generateActivationToken(emailId);
            AuthToken newVerificationToken = new AuthToken(newToken, emailId,
                    Instant.now().plusMillis(jwtService.getActivationTokenExpirationTime()));
            authTokenRepository.save(newVerificationToken);

            try {
                emailService.sendVerificationEmail(emailId, "Account Verification", newToken);
            } catch (MessagingException e) {
                log.error("Failed to send new verification email to {}", emailId, e);
                throw new EmailSendingException(messageSource.getMessage("email.verification.resend.failed", null, LocaleContextHolder.getLocale()), e);
            }

            return messageSource.getMessage("token.expired.verification.resent", null, LocaleContextHolder.getLocale());
        }

        if (!jwtService.validateToken(token, "activation", emailId)) {
            log.warn("Invalid or tampered token for email: {}", emailId);
            throw new InvalidTokenException(messageSource.getMessage("token.invalid.tampered", null, LocaleContextHolder.getLocale()));
        }

        User user = userRepository.findByEmail(emailId)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", emailId);
                    return new UserNotFoundException(messageSource.getMessage("user.not.found", null, LocaleContextHolder.getLocale()));
                });

        user.setActive(true);
        userRepository.save(user);
        log.info("User {} marked as active", emailId);

        authTokenRepository.delete(verificationToken);
        try {
            emailService.sendVerificationSuccessEmail(emailId, "Email Verification Successful");
        } catch (MessagingException e) {
            log.error("Failed to send verification success email to {}", emailId, e);
            throw new EmailSendingException(messageSource.getMessage("email.verification.success.failed", null, LocaleContextHolder.getLocale()), e);
        }

        return messageSource.getMessage("email.verification.success", null, LocaleContextHolder.getLocale());
    }



    public String resendVerificationEmail(String emailId) throws MessagingException {
        log.info("Resending verification email to {}", emailId);

        Role role = roleRepository.findByAuthority("ROLE_CUSTOMER")
                .orElseThrow(() -> {
                    log.warn("Customer role not found");
                    return new RoleNotFoundException(messageSource.getMessage("role.not.found", null, LocaleContextHolder.getLocale()));
                });

        User user = userRepository.findByEmailAndRoles(emailId, Set.of(role))
                .orElseThrow(() -> {
                    log.warn("Customer not found with email: {}", emailId);
                    return new UserNotFoundException(messageSource.getMessage("customer.not.found", null, LocaleContextHolder.getLocale()));
                });

        if (user.isActive()) {
            log.info("User {} is already active", emailId);
            return messageSource.getMessage("user.already.active", null, LocaleContextHolder.getLocale());
        }

        authTokenRepository.deleteByEmail(emailId);
        log.debug("Old tokens deleted for {}", emailId);

        String activationToken = jwtService.generateActivationToken(emailId);
        AuthToken authToken = new AuthToken(activationToken, emailId,
                Instant.now().plusMillis(jwtService.getActivationTokenExpirationTime()));
        authTokenRepository.save(authToken);
        log.debug("New activation token saved for {}", emailId);

        try {
            emailService.sendVerificationEmail(user.getEmail(), "Account Verification", activationToken);
            log.info("Verification email sent to {}", emailId);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", emailId, e);
            throw new EmailSendingException(messageSource.getMessage("email.verification.failed", null, LocaleContextHolder.getLocale()), e);
        }

        return messageSource.getMessage("verification.email.resent.success", null, LocaleContextHolder.getLocale());
    }


    /**
     * Reset failed attempts on successful login...Generate tokens...Store refresh token in database
     */

    public LoginResponseDTO login(LoginDTO loginDTO, String requiredRole) throws AccessDeniedException {
        log.info("Attempting login for user: {}", loginDTO.getEmail());

        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", loginDTO.getEmail());
                    return new UserNotFoundException(messageSource.getMessage("user.not.found", new Object[]{loginDTO.getEmail()}, LocaleContextHolder.getLocale()));
                });

        boolean hasRequiredRole = user.getRoles().stream()
                .anyMatch(role -> role.getAuthority().equals(requiredRole));

        if (!hasRequiredRole) {
            log.warn("User {} attempted to login with incorrect endpoint for role {}", loginDTO.getEmail(), requiredRole);
            throw new AccessDeniedException(messageSource.getMessage("access.denied.message", null, LocaleContextHolder.getLocale()));
        }
        if (!user.isActive()) {
            log.warn("Account is not activated for user: {}", loginDTO.getEmail());
            throw new AccountNotActivatedException(messageSource.getMessage("account.not.activated", null, LocaleContextHolder.getLocale()));
        }

        if (user.isLocked()) {
            log.warn("Account is locked for user: {}", loginDTO.getEmail());
            throw new AccountLockedException(messageSource.getMessage("account.locked", null, LocaleContextHolder.getLocale()));
        }

        if (user.getPasswordUpdateDate().plusMonths(3).isBefore(LocalDateTime.now())) {
            user.setExpired(true);
            userRepository.save(user);
            log.warn("Password expired for user: {}", loginDTO.getEmail());
            throw new PasswordExpiredException(messageSource.getMessage("password.expired", null, LocaleContextHolder.getLocale()));
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            handleFailedLoginAttempt(user);
            log.warn("Invalid password attempt for user: {}", loginDTO.getEmail());
            throw new InvalidCredentialsException(messageSource.getMessage("invalid.password", new Object[]{(3 - user.getInvalidAttemptCount())}, LocaleContextHolder.getLocale()));
        }

        if (user.getInvalidAttemptCount() > 0) {
            user.setInvalidAttemptCount(0);
            userRepository.save(user);
            log.debug("Reset invalid attempt count for user: {}", loginDTO.getEmail());
        }

        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        String accessToken = jwtService.generateAccessToken(user.getEmail(), sessionId);

        // Store mapping between access and refresh token
        redisTemplate.opsForValue().set("session:" + sessionId, refreshToken, jwtService.getAccessTokenExpirationTime() + 60000, TimeUnit.MILLISECONDS);
        log.debug("Stored session mapping for sessionId: {}", sessionId);

        Instant refreshExpiry = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationTime());
        AuthToken authToken = new AuthToken(refreshToken, user.getEmail(), refreshExpiry);
        authToken.setTokenType(TokenType.REFRESH);
        authTokenRepository.save(authToken);
        log.debug("Refresh token saved for user: {}", loginDTO.getEmail());

        return new LoginResponseDTO(accessToken, refreshToken, "Bearer", user.getEmail(), user.getFirstName(), user.getLastName());
    }


    /**
     * Lock account after 3 failed attempts
     */
    private void handleFailedLoginAttempt(User user) {
        user.setInvalidAttemptCount(user.getInvalidAttemptCount() + 1);
        log.info("Failed login attempt #{} for user: {}", user.getInvalidAttemptCount(), user.getEmail());

        if (user.getInvalidAttemptCount() >= 3) {
            user.setLocked(true);
            log.warn("User account locked due to 3 failed login attempts: {}", user.getEmail());

            try {
                emailService.sendAccountLockedEmail(user.getEmail(), messageSource.getMessage("account.locked.email.subject", null, LocaleContextHolder.getLocale()));
            } catch (MessagingException e) {
                log.error("Failed to send account locked notification for user: {}", user.getEmail(), e);
                throw new EmailSendingException(messageSource.getMessage("email.send.failed.account.locked", null, LocaleContextHolder.getLocale()), e);
            }
        }
        userRepository.save(user);
        log.debug("User locked status and invalid attempt count saved for user: {}", user.getEmail());
    }



    /**
     * Validate refresh token exists...
     * Check token type...
     * Validate token expiration...
     * Extract email from token...
     * Verify token is valid...
     * Get user details...
     * Generate new access token
     */
    public LoginResponseDTO refreshToken(String refreshToken) {
        log.info("Attempting to refresh token for refreshToken: {}", refreshToken);

        AuthToken storedToken = authTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.error("Invalid refresh token: {}", refreshToken);
                    return new InvalidTokenException(messageSource.getMessage("invalid.refresh.token", null, LocaleContextHolder.getLocale()));
                });

        if (storedToken.getTokenType() != TokenType.REFRESH) {
            log.error("Invalid token type for refresh token: {}", refreshToken);
            throw new InvalidTokenException(messageSource.getMessage("invalid.token.type", null, LocaleContextHolder.getLocale()));
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            authTokenRepository.delete(storedToken);
            log.warn("Refresh token expired: {}", refreshToken);
            throw new InvalidTokenException(messageSource.getMessage("refresh.token.expired", null, LocaleContextHolder.getLocale()));
        }

        String email = jwtService.extractUsername(refreshToken);

        if (!jwtService.validateToken(refreshToken, "refresh", email)) {
            log.error("Invalid refresh token for user: {}", email);
            throw new InvalidTokenException(messageSource.getMessage("invalid.refresh.token", null, LocaleContextHolder.getLocale()));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UserNotFoundException(messageSource.getMessage("user.not.found", new Object[]{email}, LocaleContextHolder.getLocale()));
                });

        String sessionId = UUID.randomUUID().toString();
        String newAccessToken = jwtService.generateAccessToken(email, sessionId);

        // Store mapping between access and refresh token
        redisTemplate.opsForValue().set("session:" + sessionId, refreshToken, jwtService.getAccessTokenExpirationTime(), TimeUnit.MILLISECONDS);

        log.info("Token refreshed successfully for user: {}", email);
        return new LoginResponseDTO(newAccessToken, refreshToken, "Bearer", email, user.getFirstName(), user.getLastName());
    }


    public String logout(String token) {

        String email = jwtService.extractUsername(token);
        if (!jwtService.validateToken(token, "access", email)) {
            throw new InvalidTokenException(messageSource.getMessage("access.token.invalid", null, LocaleContextHolder.getLocale()));
        }

        //blacklisting the access token
        Instant expiresAt = jwtService.extractExpiration(token).toInstant();
        long remainingTimeMillis = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        if (remainingTimeMillis > 0) {
            tokenBlacklistService.blacklistAccessToken(token, remainingTimeMillis);
        }

        String sessionId = jwtService.extractIssuer(token);

        //deleting the refresh token from db and redis
        String sessionKey = "session:" + sessionId;
        String refreshToken = redisTemplate.opsForValue().get(sessionKey);
        if (refreshToken != null) {
            authTokenRepository.findByToken(refreshToken).ifPresent(authToken -> authTokenRepository.delete(authToken));
            redisTemplate.delete(sessionKey);
        }

        return messageSource.getMessage("logout.success", null, LocaleContextHolder.getLocale());
    }

    public String sellerSignup(SellerDTO input) {
        log.info("Starting seller signup for email: {}", input.getEmail());

        if (!input.getPassword().equals(input.getConfirmPassword())) {
            log.warn("Password mismatch for email: {}", input.getEmail());
            throw new PasswordMismatchException(messageSource.getMessage("password.mismatch", null, LocaleContextHolder.getLocale()));
        }
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            log.warn("Email already in use: {}", input.getEmail());
            throw new EmailAlreadyInUseException(messageSource.getMessage("email.in.use", new Object[]{input.getEmail()}, LocaleContextHolder.getLocale()));
        }
        if (userRepository.existsByGstin(input.getGstin())) {
            log.warn("GST already in use: {}", input.getGstin());
            throw new GstAlreadyInUseException(messageSource.getMessage("gst.in.use", new Object[]{input.getGstin()}, LocaleContextHolder.getLocale()));
        }

        if (userRepository.existsByCompanyName(input.getCompanyName())) {
            log.warn("Company already exists: {}", input.getCompanyName());
            throw new DuplicateResourceException(messageSource.getMessage("company.already.exists", new Object[]{input.getCompanyName()}, LocaleContextHolder.getLocale()));
        }

        Seller seller = new Seller();
        seller.setFirstName(input.getFirstName());
        seller.setLastName(input.getLastName());
        seller.setEmail(input.getEmail());
        seller.setPassword(passwordEncoder.encode(input.getPassword()));
        seller.setPasswordUpdateDate(LocalDateTime.now());
        seller.setCompanyName(input.getCompanyName());
        seller.setGstin(input.getGstin());
        seller.setCompanyContact(input.getCompanyContact());

        if (input.getMiddleName() != null && !input.getMiddleName().isEmpty()) {
            seller.setMiddleName(input.getMiddleName());
        }

        // Set the role for the seller
        Role role = roleRepository.findByAuthority("ROLE_SELLER")
                .orElseThrow(() -> {
                    log.error("Seller role not found");
                    throw new RoleNotFoundException(messageSource.getMessage("role.not.found", null, LocaleContextHolder.getLocale()));
                });
        seller.addRole(role);

        Address address = new Address();
        address.setAddressLine(input.getAddressLine());
        address.setCity(input.getCity());
        address.setState(input.getState());
        address.setCountry(input.getCountry());
        address.setZipCode(input.getZipCode());
        address.setLabel("Office");

        seller.getAddresses().add(address);

        userRepository.save(seller);
        log.info("Seller saved successfully: {}", input.getEmail());

        // Send success email
        try {
            emailService.sendSuccessEmailToSeller(input.getEmail(), messageSource.getMessage("email.verification.success", null, LocaleContextHolder.getLocale()));
            log.info("Success email sent to seller: {}", input.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send success email to seller: {}", input.getEmail(), e);
            throw new EmailSendingException(messageSource.getMessage("email.send.failed.account.locked", null, LocaleContextHolder.getLocale()), e);
        }

        return messageSource.getMessage("seller.signup.success", null, LocaleContextHolder.getLocale());
    }


    public String forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {

        User user = userRepository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        messageSource.getMessage("user.not.found", new Object[]{forgotPasswordDTO.getEmail()}, LocaleContextHolder.getLocale())
                ));

        if (!user.isActive()) {
            throw new UserNotActiveException(
                    messageSource.getMessage("user.not.active", new Object[]{forgotPasswordDTO.getEmail()}, LocaleContextHolder.getLocale())
            );
        }

        // Create a user-specific key in Redis to track their current reset token
        String userResetTokenKey = "user_reset_token:" + forgotPasswordDTO.getEmail();

        // If user already has a reset token, invalidate it by deleting from Redis
        String previousToken = redisTemplate.opsForValue().get(userResetTokenKey);
        if (previousToken != null) {
            String previousTokenKey = "bl_reset_token:" + previousToken;
            redisTemplate.delete(previousTokenKey);
        }

        // Generate a new reset password token
        String token = jwtService.generateResetPasswordToken(user.getEmail());

        long tokenExpirationMillis = jwtService.getResetPasswordTokenExpirationTime();
        tokenBlacklistService.blacklistResetToken(token, tokenExpirationMillis);

        // Store reference to the user's current reset token in Redis
        redisTemplate.opsForValue().set(userResetTokenKey, token, tokenExpirationMillis, TimeUnit.MILLISECONDS);

        // Send reset password email
        try {
            emailService.sendResetPasswordEmail(user.getEmail(),
                    messageSource.getMessage("email.reset.subject", null, LocaleContextHolder.getLocale()),
                    token);
        } catch (MessagingException e) {
            throw new EmailSendingException(
                    messageSource.getMessage("email.send.failed.reset", null, LocaleContextHolder.getLocale()), e
            );
        }

        return messageSource.getMessage("password.reset.link.sent", null, LocaleContextHolder.getLocale());
    }

    public String resetPassword(ResetPasswordDTO resetPasswordDTO) {
        // Check if token exists
        if (resetPasswordDTO.getToken() == null || resetPasswordDTO.getToken().isBlank()) {
            throw new InvalidTokenException(
                    messageSource.getMessage("reset.token.missing", null, LocaleContextHolder.getLocale())
            );
        }

        // Check password match
        if (!resetPasswordDTO.getPassword().equals(resetPasswordDTO.getConfirmPassword())) {
            throw new PasswordMismatchException(
                    messageSource.getMessage("password.mismatch", null, LocaleContextHolder.getLocale())
            );
        }

        try {
            // First attempt to extract claims to catch malformed tokens
            String email = jwtService.extractUsername(resetPasswordDTO.getToken());

            // Check token type explicitly before other validations
            String tokenType = jwtService.getTokenType(resetPasswordDTO.getToken());
            if (!"forgot".equals(tokenType)) {
                throw new InvalidTokenException(
                        messageSource.getMessage("invalid.token.type", new Object[]{tokenType}, LocaleContextHolder.getLocale())
                );
            }

            // Check expiration separately
            if (jwtService.isTokenExpired(resetPasswordDTO.getToken())) {
                throw new ExpiredTokenException(
                        messageSource.getMessage("token.expired", null, LocaleContextHolder.getLocale())
                );
            }

            // Validate token
            if (!jwtService.validateToken(resetPasswordDTO.getToken(), "forgot", email)) {
                throw new InvalidTokenException(
                        messageSource.getMessage("token.invalid", null, LocaleContextHolder.getLocale())
                );
            }

            // Check if token is in Redis store
            String tokenKey = "bl_reset_token:" + resetPasswordDTO.getToken();
            String storedToken = tokenBlacklistService.getStoredToken(tokenKey);

            if (storedToken == null) {
                throw new InvalidTokenException(
                        messageSource.getMessage("token.not.found.or.used", null, LocaleContextHolder.getLocale())
                );
            }

            // Get user and reset password
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(
                            messageSource.getMessage("user.not.found", new Object[]{email}, LocaleContextHolder.getLocale())
                    ));

            user.setPassword(passwordEncoder.encode(resetPasswordDTO.getPassword()));

            if (user.isLocked()) {
                user.setLocked(false);
                user.setInvalidAttemptCount(0);
            }

            user.setPasswordUpdateDate(LocalDateTime.now());
            user.setExpired(false);
            userRepository.save(user);

            // Remove used token
            redisTemplate.delete(tokenKey);

            return messageSource.getMessage("password.reset.success", null, LocaleContextHolder.getLocale());

        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException(
                    messageSource.getMessage("token.expired", null, LocaleContextHolder.getLocale())
            );
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException(
                    messageSource.getMessage("token.malformed", null, LocaleContextHolder.getLocale())
            );
        } catch (io.jsonwebtoken.security.SecurityException e) {
            throw new InvalidTokenException(
                    messageSource.getMessage("token.invalid", null, LocaleContextHolder.getLocale())
            );
        } catch (JwtException e) {
            throw new InvalidTokenException(
                    messageSource.getMessage("token.invalid", new Object[]{e.getMessage()}, LocaleContextHolder.getLocale())
            );
        }
    }


}

