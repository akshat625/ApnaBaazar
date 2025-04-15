package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.service.CustomerService;
import com.apnabaazar.apnabaazar.service.S3Service;
import com.apnabaazar.apnabaazar.service.SellerService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;
    private final S3Service s3Service;

    @GetMapping("/hello")
    public String testCustomer() {
        return "Hello World! from Customer";
    }

    @GetMapping("/profile")
    public ResponseEntity<CustomerProfileDTO> getCustomerProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return customerService.getCustomerProfile(userPrincipal);
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getCustomerAddresses(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return customerService.getCustomerAddresses(userPrincipal);
    }

    @PostMapping("/addresses")
    public ResponseEntity<GenericResponseDTO> addCustomerAddress(@AuthenticationPrincipal UserPrincipal userPrincipal, AddressDTO  addressDTO) {
        customerService.addCustomerAddress(userPrincipal,addressDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, "Address added successfully."));
    }


    @PostMapping("/upload/profile-image")
    public ResponseEntity<GenericResponseDTO> uploadCustomerProfileImage(@RequestParam MultipartFile file, @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        String key = s3Service.uploadProfileImage(userPrincipal.getUsername(), file);
        return ResponseEntity.ok(new GenericResponseDTO(true, "Image uploaded at key : " + key));

    }

    @DeleteMapping("/profile/image")
    public ResponseEntity<GenericResponseDTO> deleteCustomerProfileImage(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        String username = userPrincipal.getUsername();
        boolean deleted = s3Service.deleteProfileImage(username);
        if (deleted) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Profile image deleted successfully."));
        } else {
            return ResponseEntity.ok(new GenericResponseDTO(true, "No profile image found to delete."));
        }
    }

    @PutMapping("/address/{addressId}")
    public ResponseEntity<GenericResponseDTO> updateCustomerAddress(UserPrincipal userPrincipal, @PathVariable String addressId, @Valid @RequestBody AddressUpdateDTO addressUpdateDTO) {
        customerService.updateCustomerAddress(userPrincipal, addressId, addressUpdateDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, "Address updated successfully."));
    }

    @PutMapping("/password")
    public ResponseEntity<GenericResponseDTO> updateCustomerPassword(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        customerService.updateCustomerPassword(userPrincipal, updatePasswordDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, "Password updated successfully."));
    }
}
