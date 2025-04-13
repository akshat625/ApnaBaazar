package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.ResourceNotFoundException;
import com.apnabaazar.apnabaazar.mapper.CustomerMapper;
import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomerService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthTokenRepository authTokenRepository;
    private final AddressRepository addressRepository;

    @Value("${aws.s3.default-customer-image}")
    private String defaultCustomerImage;

    public ResponseEntity<CustomerProfileDTO> getCustomerProfile(UserPrincipal userPrincipal) {

        String email = userPrincipal.getUsername();
        log.info("Fetching profile for customer: {}", email);

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Customer not found with email: {}", email);
                    return new UsernameNotFoundException("Customer not found");
                });

        try{
            String imageUrl = s3Service.getProfileImageUrl(email,defaultCustomerImage);
            log.info("Customer profile image URL resolved: {}", imageUrl);
            return ResponseEntity.ok(CustomerMapper.toCustomerProfileDTO(customer,imageUrl));
        }
        catch (Exception e){
            log.error("Error retrieving customer profile for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<List<AddressDTO>> getCustomerAddresses(UserPrincipal userPrincipal) {
        String email = userPrincipal.getUsername();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Customer not found with email: {}", email);
                    return new UsernameNotFoundException("Customer not found");
                });

        log.info("Fetching addresses of Customer : {}", email);
        Set<Address> customerAddresses = customer.getAddresses();
        if (customerAddresses.isEmpty()) {
            log.info("No addresses found for customer: {}", email);
        }

        return ResponseEntity.ok(CustomerMapper.toAddressDTO(customerAddresses));
    }

    private String getUpdatedValue(String newValue, String oldValue) {
        return (newValue != null && !newValue.isBlank()) ? newValue : oldValue;
    }

    public void updateCustomerAddress(UserPrincipal userPrincipal,String addressId, AddressUpdateDTO addressUpdateDTO) {
        String email = userPrincipal.getUsername();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address not found with ID: " + addressId));
        log.info("Updating address [ID: {}] for customer: {}", addressId, email);

        if (addressUpdateDTO != null){
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
}




