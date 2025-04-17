package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.PasswordMismatchException;
import com.apnabaazar.apnabaazar.exceptions.ResourceNotFoundException;
import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.ProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.AddressRepository;
import com.apnabaazar.apnabaazar.repository.SellerRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SellerService {

    private final SellerRepository sellerRepository;
    private final AddressRepository addressRepository;
    private final S3Service s3Service;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MessageSource messageSource;



    @Value("${aws.s3.default-seller-image}")
    private String defaultSellerImage;

    public ResponseEntity<SellerProfileDTO> getSellerProfile(UserPrincipal userPrincipal) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        log.info("Fetching profile for seller: {}", email);

        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Seller not found with email: {}", email);
                    return new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale));
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


    public void updateSellerProfile(UserPrincipal userPrincipal, ProfileUpdateDTO sellerProfileUpdateDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        log.info("Updating profile for seller: {}", email);
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));
        if (sellerProfileUpdateDTO != null){
            seller.setFirstName(getUpdatedValue(sellerProfileUpdateDTO.getFirstName(), seller.getFirstName()));
            seller.setMiddleName(getUpdatedValue(sellerProfileUpdateDTO.getMiddleName(), seller.getMiddleName()));
            seller.setLastName(getUpdatedValue(sellerProfileUpdateDTO.getLastName(), seller.getLastName()));
            seller.setCompanyContact(getUpdatedValue(sellerProfileUpdateDTO.getContact(), seller.getCompanyContact()));
       }
        sellerRepository.save(seller);
        log.info("Seller profile updated successfully for: {}", email);
    }

    public void updateSellerAddress(UserPrincipal userPrincipal,String addressId, AddressUpdateDTO addressUpdateDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException(messageSource.getMessage("address.not.found", new Object[]{addressId}, locale)));
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
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        log.info("Updating seller password for seller: {}", email);
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));
        if(!passwordEncoder.matches(updatePasswordDTO.getOldPassword(),seller.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.old.incorrect", null, locale));
        if(passwordEncoder.matches(updatePasswordDTO.getNewPassword(),seller.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.new.password.incorrect", null, locale));
        if(!updatePasswordDTO.getNewPassword().equals(updatePasswordDTO.getConfirmPassword())) {
            throw new PasswordMismatchException(messageSource.getMessage("password.mismatch", null, locale));
        }

        seller.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        seller.setPasswordUpdateDate(LocalDateTime.now());
        sellerRepository.save(seller);

        log.info("Seller password updated successfully for: {}", email);
    }
}
