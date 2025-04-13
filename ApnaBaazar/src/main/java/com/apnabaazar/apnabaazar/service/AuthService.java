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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final TokenBlacklistService tokenBlacklistService;
    private final RedisTemplate<String, String> redisTemplate;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private JwtService jwtService;
    private AuthTokenRepository authTokenRepository;

    @Autowired
    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, EmailService emailService, JwtService jwtService, AuthTokenRepository authTokenRepository, TokenBlacklistService tokenBlacklistService, RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.authTokenRepository = authTokenRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.redisTemplate = redisTemplate;
    }

    public String customerSignup(CustomerDTO input) {
        if (!input.getPassword().equals(input.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords don't match");
        }
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new EmailAlreadyInUseException("Email already in use");
        }
        Customer customer = new Customer();
        customer.setEmail(input.getEmail());
        customer.setPassword(passwordEncoder.encode(input.getPassword()));
        customer.setFirstName(input.getFirstName());
        customer.setLastName(input.getLastName());
        customer.setContact(input.getContact());

        Role role = roleRepository.findByAuthority("ROLE_CUSTOMER")
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));
        customer.addRole(role);

        if (input.getMiddleName() != null && !input.getMiddleName().isEmpty()) {
            customer.setMiddleName(input.getMiddleName());
        }
        userRepository.save(customer);

        String activationToken = jwtService.generateActivationToken(customer.getEmail());


        AuthToken authToken = new AuthToken(activationToken, customer.getEmail(), Instant.now().plusMillis(jwtService.getActivationTokenExpirationTime()));
        authTokenRepository.save(authToken);

        try {
            emailService.sendVerificationEmail(customer.getEmail(), "Account Verification", activationToken);
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send verification email", e);
        }

        return "User registered successfully";
    }


    public String verifyUser(String token) {
        String emailId = jwtService.extractUsername(token);



        AuthToken verificationToken = authTokenRepository.findByToken(token)
                .orElseThrow(() -> new VerificationTokenNotFoundException("Token not found or already used."));

        if (jwtService.isTokenExpired(token)) {
            authTokenRepository.delete(verificationToken);
            String newToken = jwtService.generateActivationToken(emailId);
            AuthToken newVerificationToken = new AuthToken(newToken, emailId, Instant.now().plusMillis(jwtService.getActivationTokenExpirationTime()));
            authTokenRepository.save(newVerificationToken);
            try {
                emailService.sendVerificationEmail(emailId, "Account Verification", newToken);
            } catch (MessagingException e) {
                throw new EmailSendingException("Failed to send new verification email.", e);
            }
            return "Token expired. A new verification email has been sent.";
        }
        if (!jwtService.validateToken(token, "activation", emailId)) {
            throw new InvalidTokenException("Invalid or tampered token.");
        }

        User user = userRepository.findByEmail(emailId).orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setActive(true);
        userRepository.save(user);

        authTokenRepository.delete(verificationToken);
        try {
            emailService.sendVerificationSuccessEmail(emailId, "Email Verification Successful");
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send verification success email.", e);
        }
        return "Email verified successfully";
    }


    public String resendVerificationEmail(String emailId) throws MessagingException {
        Role role = roleRepository.findByAuthority("ROLE_CUSTOMER")
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));
        User user = userRepository.findByEmailAndRoles(emailId, Set.of(role))
                .orElseThrow(() -> new UserNotFoundException("Customer not found with this email"));
        if (user.isActive()) {
            return "User is already active";
        }
        authTokenRepository.deleteByEmail(emailId);

        String activationToken = jwtService.generateActivationToken(emailId);
        AuthToken authToken = new AuthToken(activationToken, emailId, Instant.now().plusMillis(jwtService.getActivationTokenExpirationTime()));
        authTokenRepository.save(authToken);
        try {
            emailService.sendVerificationEmail(user.getEmail(), "Account Verification", activationToken);
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send verification email", e);
        }

        return "Verification email resent successfully";
    }

    /**
     * Reset failed attempts on successful login...Generate tokens...Store refresh token in database
     */

    public LoginResponseDTO login(LoginDTO loginDTO) {

        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + loginDTO.getEmail()));

        if (!user.isActive()) {
            throw new AccountNotActivatedException("Account is not activated. Please verify your email.");
        }

        if (user.isLocked()) {
            throw new AccountLockedException("Account is locked due to multiple failed attempts. Please try later.");
        }

        if (user.isExpired()) {
            throw new PasswordExpiredException("Your password has expired. Please reset your password.");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            handleFailedLoginAttempt(user);
            throw new InvalidCredentialsException("Invalid password. You have "+ (3-user.getInvalidAttemptCount())+ " attempts left.");
        }


        if (user.getInvalidAttemptCount() > 0) {
            user.setInvalidAttemptCount(0);
            userRepository.save(user);
        }

        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        String accessToken = jwtService.generateAccessToken(user.getEmail(), sessionId);

        //store mapping between access and refresh token
        redisTemplate.opsForValue().set("session:" + sessionId, refreshToken, jwtService.getAccessTokenExpirationTime() + 60000, TimeUnit.MILLISECONDS);

        Instant refreshExpiry = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationTime());
        AuthToken authToken = new AuthToken(refreshToken, user.getEmail(), refreshExpiry);
        authToken.setTokenType(TokenType.REFRESH);
        authTokenRepository.save(authToken);

        return new LoginResponseDTO(accessToken, refreshToken, "Bearer", user.getEmail(), user.getFirstName(), user.getLastName());
    }

    /**
     * Lock account after 3 failed attempts
     */
    private void handleFailedLoginAttempt(User user) {
        user.setInvalidAttemptCount(user.getInvalidAttemptCount() + 1);
        if (user.getInvalidAttemptCount() >= 3) {

            user.setLocked(true);
            try {
                emailService.sendAccountLockedEmail(user.getEmail(), "Account Locked");
            } catch (MessagingException e) {
                throw new EmailSendingException("Failed to send account locked notification", e);
            }
        }
        userRepository.save(user);
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
        AuthToken storedToken = authTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (storedToken.getTokenType() != TokenType.REFRESH) {
            throw new InvalidTokenException("Invalid token type");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            authTokenRepository.delete(storedToken);
            throw new InvalidTokenException("Refresh token expired");
        }

        String email = jwtService.extractUsername(refreshToken);

        if (!jwtService.validateToken(refreshToken, "refresh", email)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));


        String sessionId = UUID.randomUUID().toString();
        String newAccessToken = jwtService.generateAccessToken(email, sessionId);

        //store mapping between access and refresh token
        redisTemplate.opsForValue().set("session:" + sessionId, refreshToken, jwtService.getAccessTokenExpirationTime(), TimeUnit.MILLISECONDS);


        return new LoginResponseDTO(newAccessToken, refreshToken, "Bearer", email, user.getFirstName(), user.getLastName());
    }


    public String logout(String token) {

        String email = jwtService.extractUsername(token);
        if (!jwtService.validateToken(token, "access", email)) {
            throw new InvalidTokenException("Invalid access token");
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

        return "Logged out";
    }


    public String sellerSignup(SellerDTO input) {

        if (!input.getPassword().equals(input.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords don't match");
        }
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new EmailAlreadyInUseException("Email already in use");
        }
        if (userRepository.existsByGstin(input.getGstin())) {
            throw new GstAlreadyInUseException("Gst already in use");
        }
        if (userRepository.existsByCompanyName(input.getCompanyName())) {
            throw new DuplicateResourceException("Company already exist");
        }


        Seller seller = new Seller();
        seller.setFirstName(input.getFirstName());
        seller.setLastName(input.getLastName());
        seller.setEmail(input.getEmail());
        seller.setPassword(passwordEncoder.encode(input.getPassword()));
        seller.setCompanyName(input.getCompanyName());
        seller.setGstin(input.getGstin());
        seller.setCompanyContact(input.getCompanyContact());
        if (input.getMiddleName() != null && !input.getMiddleName().isEmpty()) {
            seller.setMiddleName(input.getMiddleName());
        }

        Role role = roleRepository.findByAuthority("ROLE_SELLER")
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));
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

        try {
            emailService.sendSuccessEmailToSeller(input.getEmail(), "Account Created");
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send success email to seller", e);
        }
        return "Seller registered successfully!";
    }


    public String forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {

        User user = userRepository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new UserNotActiveException("User is not active with this email.");
        }


        // Create a user-specific key in Redis to track their current reset token
        String userResetTokenKey = "user_reset_token:" + forgotPasswordDTO.getEmail();

        // If user already has a reset token, invalidate it by deleting from Redis
        String previousToken = redisTemplate.opsForValue().get(userResetTokenKey);
        if (previousToken != null) {
            String previousTokenKey = "bl_reset_token:" + previousToken;
            redisTemplate.delete(previousTokenKey);
        }

        String token = jwtService.generateResetPasswordToken(user.getEmail());

        long tokenExpirationMillis = jwtService.getResetPasswordTokenExpirationTime();
        tokenBlacklistService.blacklistResetToken(token, tokenExpirationMillis);

        // Store reference to the user's current reset token
        redisTemplate.opsForValue().set(userResetTokenKey, token, tokenExpirationMillis, TimeUnit.MILLISECONDS);

        try {
            emailService.sendResetPasswordEmail(user.getEmail(), "Reset Password", token);
        } catch (MessagingException e) {
            throw new EmailSendingException("Failed to send password reset email", e);
        }
        return "Password reset link has been sent to your email address";
    }

    public String resetPassword(ResetPasswordDTO resetPasswordDTO) {
        // Check if token exists
        if (resetPasswordDTO.getToken() == null || resetPasswordDTO.getToken().isBlank()) {
            throw new InvalidTokenException("Reset token is missing");
        }

        // Check password match
        if (!resetPasswordDTO.getPassword().equals(resetPasswordDTO.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords don't match");
        }

        try {
            // First attempt to extract claims to catch malformed tokens
            String email = jwtService.extractUsername(resetPasswordDTO.getToken());

            // Check token type explicitly before other validations
            String tokenType = jwtService.getTokenType(resetPasswordDTO.getToken());
            if (!"forgot".equals(tokenType)) {
                throw new InvalidTokenException("Invalid token type. Expected 'forgot' but got '" + tokenType + "'");
            }

            // Check expiration separately
            if (jwtService.isTokenExpired(resetPasswordDTO.getToken())) {
                throw new ExpiredTokenException("Token has expired");
            }

            // Validate token
            if (!jwtService.validateToken(resetPasswordDTO.getToken(), "forgot", email)) {
                throw new InvalidTokenException("Token is not valid");
            }

            // Check if token is in Redis store
            String tokenKey = "bl_reset_token:" + resetPasswordDTO.getToken();
            String storedToken = tokenBlacklistService.getStoredToken(tokenKey);

            if (storedToken == null) {
                throw new InvalidTokenException("Reset token not found or already used");
            }

            // Get user and reset password
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            user.setPassword(passwordEncoder.encode(resetPasswordDTO.getPassword()));

            if (user.isLocked()) {
                user.setLocked(false);
                user.setInvalidAttemptCount(0);
            }

            user.setPasswordUpdateDate(LocalDateTime.now());
            userRepository.save(user);

            // Remove used token
            redisTemplate.delete(tokenKey);

            return "Password reset successfully";

        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("Token has expired");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("Malformed token");
        } catch (io.jsonwebtoken.security.SecurityException e) {
            throw new InvalidTokenException("Invalid token");
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid token: " + e.getMessage());
        }
    }


}

