package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.model.dto.CustomerDTO;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Role;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CustomerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtService jwtService;



    public String customerSignup(CustomerDTO input) throws MessagingException {
        if (!input.getPassword().equals(input.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords don't match");
        }
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        Customer customer = new Customer();
        customer.setEmail(input.getEmail());
        customer.setPassword(passwordEncoder.encode(input.getPassword()));
        customer.setFirstName(input.getFirstName());
        customer.setLastName(input.getLastName());
        customer.setContact(input.getContact());
//        customer.setRoles(new HashSet<>(List.of(new Role())));
        if (input.getMiddleName() != null && !input.getMiddleName().isEmpty()) {
            customer.setMiddleName(input.getMiddleName());
        }
        userRepository.save(customer);

        String activationToken = jwtService.generateToken(input.getEmail(),"activation");
        emailService.sendVerificationEmail(customer.getEmail(), "Account Verification", activationToken);
        return "User registered successfully";
    }

    public String verifyUser(String token) throws MessagingException {
        String emailId = jwtService.extractUsername(token);

        if (jwtService.isTokenExpired(token)) {
            jwtService.invalidateToken(token);
            String activationToken = jwtService.generateToken(emailId, "activation");
            emailService.sendVerificationEmail(emailId, "Account Verification", activationToken);
            return "Token expired. A new verification email has been sent.";
        }
        if (!jwtService.validateToken(token, "activation", emailId)) {
            throw new RuntimeException("Invalid Token ");
        }

        User user = userRepository.findByEmail(emailId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepository.save(user);
        jwtService.invalidateToken(token);
        emailService.sendVerificationSuccessEmail(emailId, "Email Verification Successful");

        return "Email verified successfully";
    }

    public String resendVerificationEmail(String emailId) throws MessagingException {
        User user = userRepository.findByEmail(emailId).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.isActive()) {
            return "User is already active";
        }

// jwtUtilService.invalidateToken();
        String activationToken = jwtService.generateToken(user.getEmail(), "activation");
        emailService.sendVerificationEmail(
                user.getEmail(),
                "Account Verification",
                activationToken
        );

        return "Verification email resent successfully";
    }
}

