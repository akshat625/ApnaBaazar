package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.model.dto.CustomerDTO;
import com.apnabaazar.apnabaazar.model.dto.LoginDTO;
import com.apnabaazar.apnabaazar.model.token.AuthToken;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Role;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.RoleRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import com.apnabaazar.apnabaazar.repository.AuthTokenRepository;
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

        String activationToken = jwtService.generateToken(customer.getEmail());


        AuthToken authToken = new AuthToken(activationToken, customer.getEmail(), Instant.now().plusMillis(jwtService.getExpirationTime()));
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
            String newToken = jwtService.generateToken(emailId);
            AuthToken newVerificationToken = new AuthToken(newToken, emailId, Instant.now().plusMillis(jwtService.getExpirationTime()));
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

        String activationToken = jwtService.generateToken(emailId);
        AuthToken authToken = new AuthToken(activationToken, emailId, Instant.now().plusMillis(jwtService.getExpirationTime()));
        authTokenRepository.save(authToken);
        emailService.sendVerificationEmail(user.getEmail(),"Account Verification", activationToken);

        return "Verification email resent successfully";
    }



    public String login(LoginDTO loginDTO) {
        return null;
    }
}

