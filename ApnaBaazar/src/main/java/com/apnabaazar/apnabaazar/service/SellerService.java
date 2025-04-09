package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.model.dto.SellerDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Role;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.RoleRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class SellerService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private JwtService jwtService;

    public SellerService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, JwtService jwtService, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.roleRepository = roleRepository;
    }

    public String sellerSignup(SellerDTO input) throws MessagingException {
        if(!input.getPassword().equals(input.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords don't match");
        }
        if(userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new EmailAlreadyInUseException("Email already in use");
        }
        if(userRepository.existsByGst(input.getGst())) {
            throw new GstAlreadyInUseException("Gst already in use");
        }
        if(userRepository.existsByCompanyName(input.getCompanyName())) {
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
                .orElseThrow(()->new RoleNotFoundException("Role not found"));
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

        emailService.sendSuccessEmailToSeller(input.getEmail(),"Account Created");

        return "Seller registered successfully!";
    }
}
