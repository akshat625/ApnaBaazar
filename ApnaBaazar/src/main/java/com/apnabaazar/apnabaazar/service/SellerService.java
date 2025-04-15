package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.PasswordMismatchException;
import com.apnabaazar.apnabaazar.exceptions.ResourceNotFoundException;
import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.AddressRepository;
import com.apnabaazar.apnabaazar.repository.SellerRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SellerService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final EmailService emailService;
    private final AddressRepository addressRepository;
    private final S3Service s3Service;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${aws.s3.default-seller-image}")
    private String defaultSellerImage;

    public ResponseEntity<SellerProfileDTO> getSellerProfile(UserPrincipal userPrincipal) {
        String email = userPrincipal.getUsername();
        log.info("Fetching profile for seller: {}", email);

        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Seller not found with email: {}", email);
                    return new UsernameNotFoundException("Seller not found");
                });

        try {
            String imageUrl = s3Service.getProfileImageUrl(email, defaultSellerImage);
            log.info("Seller profile image URL resolved: {}", imageUrl);

            return ResponseEntity.ok(SellerMapper.toSellerProfileDTO(seller,imageUrl));
        } catch (Exception e) {
            log.error("Error retrieving seller profile for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getUpdatedValue(String newValue, String oldValue) {
        return (newValue != null && !newValue.isBlank()) ? newValue : oldValue;
    }


    public void updateSellerProfile(UserPrincipal userPrincipal, SellerProfileUpdateDTO sellerProfileUpdateDTO) {
        String email = userPrincipal.getUsername();
        log.info("Updating seller profile for seller: {}", email);
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Seller not found"));
        if (sellerProfileUpdateDTO != null){
            seller.setFirstName(getUpdatedValue(sellerProfileUpdateDTO.getFirstName(), seller.getFirstName()));
            seller.setMiddleName(getUpdatedValue(sellerProfileUpdateDTO.getMiddleName(), seller.getMiddleName()));
            seller.setLastName(getUpdatedValue(sellerProfileUpdateDTO.getLastName(), seller.getLastName()));
            seller.setCompanyContact(getUpdatedValue(sellerProfileUpdateDTO.getCompanyContact(), seller.getCompanyContact()));
       }
        sellerRepository.save(seller);
        log.info("Seller profile updated successfully for: {}", email);
    }

    public void updateSellerAddress(UserPrincipal userPrincipal,String addressId, AddressUpdateDTO addressUpdateDTO) {
        String email = userPrincipal.getUsername();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address not found with ID: " + addressId));
        log.info("Updating address [ID: {}] for seller: {}", addressId, email);

        if (addressUpdateDTO != null){
            address.setAddressLine(getUpdatedValue(addressUpdateDTO.getAddressLine(), address.getAddressLine()));
            address.setCity(getUpdatedValue(addressUpdateDTO.getCity(), address.getCity()));
            address.setState(getUpdatedValue(addressUpdateDTO.getState(), address.getState()));
            address.setZipCode(getUpdatedValue(addressUpdateDTO.getZipCode(), address.getZipCode()));
            address.setCountry(getUpdatedValue(addressUpdateDTO.getCountry(), address.getCountry()));
        }
        addressRepository.save(address);
        log.info("Address [ID: {}] updated successfully for seller: {}", addressId, email);
    }

    public void updateSellerPassword(UserPrincipal userPrincipal, UpdatePasswordDTO updatePasswordDTO) {
        String email = userPrincipal.getUsername();
        log.info("Updating seller password for seller: {}", email);
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Seller not found"));
        if(!passwordEncoder.matches(updatePasswordDTO.getOldPassword(),seller.getPassword()))
            throw new PasswordMismatchException("Old Password is incorrect.");
        if(!updatePasswordDTO.getNewPassword().equals(updatePasswordDTO.getConfirmPassword())) {
            throw new PasswordMismatchException("New password and confirm password do not match.");
        }

        seller.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        seller.setPasswordUpdateDate(LocalDateTime.now());
        sellerRepository.save(seller);

        log.info("Seller password updated successfully for: {}", email);
    }
}
