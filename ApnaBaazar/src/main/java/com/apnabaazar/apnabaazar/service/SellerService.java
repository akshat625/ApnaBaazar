package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.ResourceNotFoundException;
import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.AddressRepository;
import com.apnabaazar.apnabaazar.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SellerService {

    private final SellerRepository sellerRepository;
    private final EmailService emailService;
    private final AddressRepository addressRepository;
    private final S3Service s3Service;

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
            String imageUrl = s3Service.getSellerProfileImageUrl(email, defaultSellerImage);
            log.info("Seller profile image URL resolved: {}", imageUrl);

            return SellerMapper.toSellerProfileDTO(seller, imageUrl);
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

    public void updateSellerAddress(String addressId, AddressDTO addressDTO) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address not found with ID: " + addressId));
        log.info("Updating seller address for seller: {}", address);
        if (addressDTO != null){
            address.setAddressLine(getUpdatedValue(addressDTO.getAddressLine(), address.getAddressLine()));
            address.setCity(getUpdatedValue(addressDTO.getCity(), address.getCity()));
            address.setState(getUpdatedValue(addressDTO.getState(), address.getState()));
            address.setZipCode(getUpdatedValue(addressDTO.getZipCode(), address.getZipCode()));
            address.setCountry(getUpdatedValue(addressDTO.getCountry(), address.getCountry()));
        }
        addressRepository.save(address);
        log.info("Address updated successfully for seller: {}", address);
    }
}
