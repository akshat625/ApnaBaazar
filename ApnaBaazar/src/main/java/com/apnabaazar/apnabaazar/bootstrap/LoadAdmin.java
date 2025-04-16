package com.apnabaazar.apnabaazar.bootstrap;

import com.apnabaazar.apnabaazar.exceptions.RoleNotFoundException;
import com.apnabaazar.apnabaazar.model.users.Role;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.RoleRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class LoadAdmin implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${apnabaazar.admin.email}")
    private String adminEmail;

    @Value("${apnabaazar.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            Role adminRole = roleRepository.findByAuthority("ROLE_ADMIN")
                    .orElseThrow(() -> new RoleNotFoundException("Admin role not found"));

            User user = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName("Akshat")
                    .lastName("Srivastava")
                    .invalidAttemptCount(0)
                    .isActive(true)
                    .isDeleted(false)
                    .isExpired(false)
                    .isLocked(false)
                    .roles(Set.of(adminRole))
                    .build();


            userRepository.save(user);
        }
    }
}