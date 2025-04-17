package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.PasswordMismatchException;
import com.apnabaazar.apnabaazar.exceptions.ResourceNotFoundException;
import com.apnabaazar.apnabaazar.mapper.CustomerMapper;
import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.ProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MessageSource messageSource;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final AddressRepository addressRepository;


    @Value("${aws.s3.default-customer-image}")
    private String defaultCustomerImage;

    private Customer getCustomerByEmail(String email) {
        Locale locale = LocaleContextHolder.getLocale();
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Customer not found with email: {}", email);
                    String message = messageSource.getMessage("customer.not.found", new Object[]{email}, locale);
                    return new UsernameNotFoundException(message);
                });
    }

    private boolean validateAddressOwnership(Customer customer, String addressId, String email) throws AccessDeniedException {
        Locale locale = LocaleContextHolder.getLocale();
        boolean ownsAddress = customer.getAddresses().stream()
                .anyMatch(address -> address.getId().equals(addressId));
        if (!ownsAddress) {
            log.warn("Address [ID: {}] does not belong to customer: {}", addressId, email);
            String message = messageSource.getMessage("address.unauthorized", null, locale);
            throw new AccessDeniedException(message);
        }
        return true;
    }

    private Address getAddressById(String addressId) {
        Locale locale = LocaleContextHolder.getLocale();
        return addressRepository.findById(addressId)
                .orElseThrow(() -> {
                    String message = messageSource.getMessage("address.not.found", new Object[]{addressId}, locale);
                    return new ResourceNotFoundException(message);
                });
    }

    private String getUpdatedValue(String newValue, String oldValue) {
        return (newValue != null && !newValue.isBlank()) ? newValue : oldValue;
    }

//---------------------------------------------------------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------------------------------------------------------

    public ResponseEntity<CustomerProfileDTO> getCustomerProfile(UserPrincipal userPrincipal) {

        String email = userPrincipal.getUsername();
        log.info("Fetching profile for customer: {}", email);

        Customer customer = getCustomerByEmail(email);

        try {
            String imageUrl = s3Service.getProfileImageUrl(email, defaultCustomerImage);
            log.info("Customer profile image URL resolved: {}", imageUrl);
            return ResponseEntity.ok(CustomerMapper.toCustomerProfileDTO(customer, imageUrl));
        } catch (Exception e) {
            log.error("Error retrieving customer profile for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<List<AddressDTO>> getCustomerAddresses(UserPrincipal userPrincipal) {
        String email = userPrincipal.getUsername();
        Customer customer = getCustomerByEmail(email);

        log.info("Fetching addresses of Customer : {}", email);
        Set<Address> customerAddresses = customer.getAddresses();
        if (customerAddresses.isEmpty()) {
            log.info("No addresses found for customer: {}", email);
        }
        return ResponseEntity.ok(CustomerMapper.toAllAddressDTO(customerAddresses));
    }



    public void updateCustomerAddress(UserPrincipal userPrincipal, String addressId, AddressUpdateDTO addressUpdateDTO) throws AccessDeniedException {
        String email = userPrincipal.getUsername();
        Customer customer = getCustomerByEmail(email);
        validateAddressOwnership(customer, addressId, email);
        Address address = getAddressById(addressId);

        log.info("Updating address [ID: {}] for customer: {}", addressId, email);


        if (addressUpdateDTO != null) {
            address.setAddressLine(getUpdatedValue(addressUpdateDTO.getAddressLine(), address.getAddressLine()));
            address.setCity(getUpdatedValue(addressUpdateDTO.getCity(), address.getCity()));
            address.setState(getUpdatedValue(addressUpdateDTO.getState(), address.getState()));
            address.setZipCode(getUpdatedValue(addressUpdateDTO.getZipCode(), address.getZipCode()));
            address.setCountry(getUpdatedValue(addressUpdateDTO.getCountry(), address.getCountry()));
            address.setLabel(getUpdatedValue(addressUpdateDTO.getLabel(), address.getLabel()));
        }
        addressRepository.save(address);
        log.info("Address [ID: {}] updated successfully for customer: {}", addressId, email);
    }


    public void updateCustomerPassword(UserPrincipal userPrincipal, UpdatePasswordDTO updatePasswordDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        log.info("Updating password for customer: {}", email);
        Customer customer = getCustomerByEmail(email);

        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), customer.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.old.incorrect", null, locale));
        if(passwordEncoder.matches(updatePasswordDTO.getNewPassword(),customer.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.new.password.incorrect", null, locale));
        if (!updatePasswordDTO.getNewPassword().equals(updatePasswordDTO.getConfirmPassword())) {
            throw new PasswordMismatchException(messageSource.getMessage("password.mismatch", null, locale));
        }

        customer.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        customer.setPasswordUpdateDate(LocalDateTime.now());
        customerRepository.save(customer);

        log.info("Customer password updated successfully for: {}", email);
    }

    public void addCustomerAddress(UserPrincipal userPrincipal, AddressDTO addressDTO) {
        String email = userPrincipal.getUsername();
        log.info("Attempting to add a new Address for customer: {}", email);
        Customer customer = getCustomerByEmail(email);

        Address newAddress = CustomerMapper.toAddress(addressDTO);
        log.info("Adding a new Address for customer: {}", email);
        customer.getAddresses().add(newAddress);
        customerRepository.save(customer);
    }

    public void deleteCustomerAddress(UserPrincipal userPrincipal, String addressId) throws AccessDeniedException {
        String email = userPrincipal.getUsername();
        log.info("Deleting address [ID: {}] for customer: {}", addressId, email);
        Customer customer = getCustomerByEmail(email);
        if(validateAddressOwnership(customer, addressId, email)){
            addressRepository.deleteAddressById(addressId);
            log.info("Address [ID: {}] soft deleted successfully for customer: {}", addressId, email);
        }
    }

    public void updateCustomerProfile(UserPrincipal userPrincipal, ProfileUpdateDTO customerProfileUpdateDTO) {
        String email = userPrincipal.getUsername();
        log.info("Updating profile for customer: {}", email);
        Customer customer = getCustomerByEmail(email);

        if (customerProfileUpdateDTO != null) {
            customer.setFirstName(getUpdatedValue(customerProfileUpdateDTO.getFirstName(), customer.getFirstName()));
            customer.setMiddleName(getUpdatedValue(customerProfileUpdateDTO.getMiddleName(), customer.getMiddleName()));
            customer.setLastName(getUpdatedValue(customerProfileUpdateDTO.getLastName(), customer.getLastName()));
            customer.setContact(getUpdatedValue(customerProfileUpdateDTO.getContact(), customer.getContact()));
        }
        customerRepository.save(customer);
        log.info("Customer profile updated successfully for: {}", email);
    }
}