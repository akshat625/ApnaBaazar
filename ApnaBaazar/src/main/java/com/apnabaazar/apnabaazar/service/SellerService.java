package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.RoleRepository;
import com.apnabaazar.apnabaazar.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class SellerService {

    private final SellerRepository sellerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;


    public ResponseEntity<SellerProfileDTO> getSellerProfile() {
        String email = SecurityContextHolder .getContext().getAuthentication().getName();
        Seller seller = sellerRepository.findByEmail(email);
        return SellerMapper.toSellerProfileDTO(seller);
    }
}
