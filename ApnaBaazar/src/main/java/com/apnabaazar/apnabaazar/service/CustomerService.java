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
}




