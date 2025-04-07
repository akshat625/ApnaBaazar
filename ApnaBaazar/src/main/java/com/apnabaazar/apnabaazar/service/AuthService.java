package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.model.dto.*;
import com.apnabaazar.apnabaazar.model.token.AuthToken;
import com.apnabaazar.apnabaazar.model.token.TokenType;
import com.apnabaazar.apnabaazar.model.users.*;
import com.apnabaazar.apnabaazar.repository.AuthTokenRepository;
import com.apnabaazar.apnabaazar.repository.RoleRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.management.relation.RoleNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final TokenBlacklistService tokenBlacklistService;
    private final RedisTemplate<String , String> redisTemplate;
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

    public String customerSignup(CustomerDTO input) throws MessagingException, RoleNotFoundException {
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

        emailService.sendVerificationEmail(customer.getEmail(), "Account Verification", activationToken);
        return "User registered successfully";
    }


    public String verifyUser(String token) throws MessagingException {
        String emailId = jwtService.extractUsername(token);

        AuthToken verificationToken = authTokenRepository.findByToken(token)
                .orElseThrow(() -> new VerificationTokenNotFoundException("Token not found or already used."));

        if (jwtService.isTokenExpired(token)) {
            authTokenRepository.delete(verificationToken);
            String newToken = jwtService.generateActivationToken(emailId);
            AuthToken newVerificationToken = new AuthToken(newToken, emailId, Instant.now().plusMillis(jwtService.getActivationTokenExpirationTime()));
            authTokenRepository.save(newVerificationToken);
            emailService.sendVerificationEmail(emailId, "Account Verification", newToken);
            return "Token expired. A new verification email has been sent.";
        }
        if (!jwtService.validateToken(token, "activation", emailId)) {
            throw new InvalidTokenException("Invalid or tampered token.");
        }

        User user = userRepository.findByEmail(emailId).orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setActive(true);
        userRepository.save(user);

        authTokenRepository.delete(verificationToken);
        emailService.sendVerificationSuccessEmail(emailId, "Email Verification Successful");

        return "Email verified successfully";
    }


    public String resendVerificationEmail(String emailId) throws MessagingException {
        User user = userRepository.findByEmail(emailId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (user.isActive()) {
            return "User is already active";
        }
        authTokenRepository.deleteByEmail(emailId);

        String activationToken = jwtService.generateActivationToken(emailId);
        AuthToken authToken = new AuthToken(activationToken, emailId, Instant.now().plusMillis(jwtService.getActivationTokenExpirationTime()));
        authTokenRepository.save(authToken);
        emailService.sendVerificationEmail(user.getEmail(), "Account Verification", activationToken);

        return "Verification email resent successfully";
    }

    /**
     * Reset failed attempts on successful login...Generate tokens...Store refresh token in database
     */

    public LoginResponseDTO login(LoginDTO loginDTO) throws MessagingException {

        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + loginDTO.getEmail()));

        System.out.println(loginDTO.getEmail());
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
            throw new InvalidCredentialsException("Invalid password");
        }

        if (user.getInvalidAttemptCount() > 0) {
            user.setInvalidAttemptCount(0);
            userRepository.save(user);
        }

        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        String accessToken = jwtService.generateAccessToken(user.getEmail(),refreshToken);

        Instant refreshExpiry = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationTime());
        AuthToken authToken = new AuthToken(refreshToken, user.getEmail(), refreshExpiry);
        authToken.setTokenType(TokenType.REFRESH);
        authTokenRepository.save(authToken);

        return new LoginResponseDTO(accessToken, refreshToken, "Bearer", user.getEmail(), user.getFirstName(), user.getLastName());
    }

    /**
     * Lock account after 3 failed attempts
     */
    private void handleFailedLoginAttempt(User user) throws MessagingException {
        user.setInvalidAttemptCount(user.getInvalidAttemptCount() + 1);
        if (user.getInvalidAttemptCount() >= 3) {
            user.setLocked(true);
            emailService.sendAccountLockedEmail(user.getEmail(),"Account Locked");
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


        String newAccessToken = jwtService.generateAccessToken(email,refreshToken);


        return new LoginResponseDTO(newAccessToken, refreshToken, "Bearer", email, user.getFirstName(), user.getLastName());
    }


    public String logout(String token) {

        String email = jwtService.extractUsername(token);
        if (!jwtService.validateToken(token, "access", email)) {
            throw new InvalidTokenException("Invalid token");
        }

        Instant expiresAt = jwtService.extractExpiration(token).toInstant();
        long remainingTimeMillis = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();

        if (remainingTimeMillis > 0) {
            tokenBlacklistService.blacklistAccessToken(token, remainingTimeMillis);
        }

        //delete refresh token of particular device
        String issuerRefreshToken = jwtService.extractIssuer(token);
        AuthToken tokenToDelete = authTokenRepository.findByToken(issuerRefreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        authTokenRepository.delete(tokenToDelete);


        return "Logged out";
    }


    public String sellerSignup(SellerDTO input) throws MessagingException, RoleNotFoundException {

        if (!input.getPassword().equals(input.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords don't match");
        }
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new EmailAlreadyInUseException("Email already in use");
        }
        if (userRepository.existsByGst(input.getGst())) {
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
        seller.setGst(input.getGst());
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

        emailService.sendSuccessEmailToSeller(input.getEmail(), "Account Created");

        return "Seller registered successfully!";
    }


    public String forgotPassword(ForgotPasswordDTO forgotPasswordDTO) throws MessagingException {

        User user = userRepository.findByEmail(forgotPasswordDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isActive() == false) {
            throw new UserNotActiveException("User is not active with this email.");
        }


        Set<String> keys = redisTemplate.keys("bl_reset_token:*");
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                if (keys.contains(user.getEmail()))
                    redisTemplate.delete(key);
            }
        }

        String token = jwtService.generateResetPasswordToken(user.getEmail());

        long tokenExpirationMillis = jwtService.getResetPasswordTokenExpirationTime();
        tokenBlacklistService.blacklistResetToken(token, tokenExpirationMillis);

        emailService.sendResetPasswordEmail(user.getEmail(), "Reset Password", token);

        return "Password reset link has been sent to your email address";
    }

    public String resetPassword(ResetPasswordDTO resetPasswordDTO) {

        if (!resetPasswordDTO.getPassword().equals(resetPasswordDTO.getConfirmPassword()))
            throw new PasswordMismatchException("Passwords don't match");

        if (jwtService.isTokenExpired(resetPasswordDTO.getToken()))
            throw new InvalidTokenException("Token is expired");

        String email = jwtService.extractUsername(resetPasswordDTO.getToken());

        if (!jwtService.validateToken(resetPasswordDTO.getToken(), "forgot", email))
            throw new InvalidTokenException("Token is not valid");

        String tokenKey = "bl_reset_token:" + resetPasswordDTO.getToken();
        String storedToken = tokenBlacklistService.getStoredToken(tokenKey);

        if (storedToken == null)
            throw new InvalidTokenException("Reset token not found or already used.");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(resetPasswordDTO.getPassword()));

        if (user.isLocked()) {
            user.setLocked(false);
            user.setInvalidAttemptCount(0);
        }

        user.setPasswordUpdateDate(LocalDateTime.now());
        userRepository.save(user);

        redisTemplate.delete(tokenKey);

        return "Password reset successfully";

    }
}

