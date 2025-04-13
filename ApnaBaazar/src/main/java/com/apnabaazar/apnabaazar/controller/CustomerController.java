package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.service.CustomerService;
import com.apnabaazar.apnabaazar.service.S3Service;
import com.apnabaazar.apnabaazar.service.SellerService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;
    private final S3Service s3Service;

    @GetMapping("/hello")
    public String testCustomer(){
        return "Hello World! from Customer";
    }

    @PostMapping("/upload/profile-image")
    public ResponseEntity<GenericResponseDTO> uploadCustomerProfileImage(@RequestParam MultipartFile file, @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        String key = s3Service.uploadProfileImage(userPrincipal.getUsername(),file);
        return ResponseEntity.ok(new GenericResponseDTO(true,"Image uploaded at key : "+key));

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
}
