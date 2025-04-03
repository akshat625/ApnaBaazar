package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.model.dto.CustomerDTO;
import com.apnabaazar.apnabaazar.model.dto.LoginDTO;
import com.apnabaazar.apnabaazar.model.dto.LoginResponseDTO;
import com.apnabaazar.apnabaazar.model.token.AuthToken;
import com.apnabaazar.apnabaazar.model.token.TokenType;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Role;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.RoleRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import com.apnabaazar.apnabaazar.repository.AuthTokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.management.relation.RoleNotFoundException;
import java.time.Instant;

@Service
public class CustomerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthTokenRepository authTokenRepository;



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
                .orElseThrow(()->new RoleNotFoundException("Role not found"));
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
        if (!jwtService.validateToken(token, emailId)) {
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
        emailService.sendVerificationEmail(user.getEmail(),"Account Verification", activationToken);

        return "Verification email resent successfully";
    }



    public LoginResponseDTO login(LoginDTO loginDTO) {

        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + loginDTO.getEmail()));

        if (!user.isActive()) {
            throw new AccountNotActivatedException("Account is not activated. Please verify your email.");
        }

        if (user.isLocked()) {
            throw new AccountLockedException("Account is locked due to multiple failed attempts. Please contact support.");
        }

        if (user.isExpired()) {
            throw new PasswordExpiredException("Your password has expired. Please reset your password.");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            handleFailedLoginAttempt(user);
            throw new InvalidCredentialsException("Invalid password");
        }

        // Reset failed attempts on successful login
        if (user.getInvalidAttemptCount() > 0) {
            user.setInvalidAttemptCount(0);
            userRepository.save(user);
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        // Store refresh token in database
        Instant refreshExpiry = Instant.now().plusMillis(jwtService.getRefreshTokenExpirationTime());
        AuthToken authToken = new AuthToken(refreshToken, user.getEmail(), refreshExpiry);
        authToken.setTokenType(TokenType.REFRESH);
        authTokenRepository.save(authToken);

        return new LoginResponseDTO(accessToken,refreshToken,"Bearer",user.getEmail(),user.getFirstName(),user.getLastName());
    }

    private void handleFailedLoginAttempt(User user) {
        user.setInvalidAttemptCount(user.getInvalidAttemptCount() + 1);

        // Lock account after 3 failed attempts
        if (user.getInvalidAttemptCount() >= 3) {
            user.setLocked(true);
        }
        userRepository.save(user);
    }


    /**
     Validate refresh token exists...
     Check token type...
     Validate token expiration...
     Extract email from token...
     Verify token is valid...
     Get user details...
     Generate new access token
     *
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

        if (!jwtService.validateToken(refreshToken, email)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(email);


        return new LoginResponseDTO(newAccessToken,refreshToken,"Bearer",email,user.getFirstName(),user.getLastName());
    }
}

