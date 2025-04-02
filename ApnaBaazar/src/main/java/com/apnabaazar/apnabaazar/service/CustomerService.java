package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.model.dto.CustomerDTO;
import com.apnabaazar.apnabaazar.model.token.UserVerificationToken;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import com.apnabaazar.apnabaazar.repository.UserVerificationTokenRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

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

    @Autowired
    private UserVerificationTokenRepository userVerificationTokenRepository;



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
//        customer.getRoles().add();
        if (input.getMiddleName() != null && !input.getMiddleName().isEmpty()) {
            customer.setMiddleName(input.getMiddleName());
        }
        userRepository.save(customer);

        String activationToken = jwtService.generateToken(customer);


        UserVerificationToken userVerificationToken = new UserVerificationToken(activationToken, customer, Instant.now().plusMillis(jwtService.getExpirationTime()));
        userVerificationTokenRepository.save(userVerificationToken);

        emailService.sendVerificationEmail(customer.getEmail(), "Account Verification", activationToken);
        return "User registered successfully";
    }




    public String verifyUser(String token) throws MessagingException {
        String emailId = jwtService.extractUsername(token);

        UserVerificationToken verificationToken = userVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found or already used."));

        if (jwtService.isTokenExpired(token)) {
            userVerificationTokenRepository.delete(verificationToken);
            String newToken = jwtService.generateToken(verificationToken.getUser());
            UserVerificationToken newVerificationToken = new UserVerificationToken(newToken, verificationToken.getUser(), Instant.now().plusMillis(jwtService.getExpirationTime()));
            userVerificationTokenRepository.save(newVerificationToken);
            emailService.sendVerificationEmail(emailId, "Account Verification", newToken);
            return "Token expired. A new verification email has been sent.";
        }
        if (!jwtService.validateToken(token, emailId)) {
            throw new RuntimeException("Invalid Token ");
        }

        User user = verificationToken.getUser();
        user.setActive(true);
        userRepository.save(user);

        userVerificationTokenRepository.delete(verificationToken);
        emailService.sendVerificationSuccessEmail(emailId, "Email Verification Successful");

        return "Email verified successfully";
    }

    public String resendVerificationEmail(String emailId) throws MessagingException {
        User user = userRepository.findByEmail(emailId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.isActive()) {
            return "User is already active";
        }
        userVerificationTokenRepository.deleteByUser(user);
        String activationToken = jwtService.generateToken(user);
        UserVerificationToken userVerificationToken = new UserVerificationToken(activationToken, user, Instant.now().plusMillis(jwtService.getExpirationTime()));
        userVerificationTokenRepository.save(userVerificationToken);
        emailService.sendVerificationEmail(user.getEmail(),"Account Verification", activationToken);

        return "Verification email resent successfully";
    }
}

