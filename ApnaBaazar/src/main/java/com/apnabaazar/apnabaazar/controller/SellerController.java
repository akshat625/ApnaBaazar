package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileUpdateDTO;
import com.apnabaazar.apnabaazar.service.S3Service;
import com.apnabaazar.apnabaazar.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/seller")
public class SellerController {

    private final SellerService sellerService;
    private final S3Service s3Service;

    @GetMapping("/test")
    public String testCustomer(){
        return "Hello World! from Seller";
    }

    @GetMapping("/profile")
    public ResponseEntity<SellerProfileDTO>  getSellerProfile(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return sellerService.getSellerProfile(userPrincipal);
    }

    @PostMapping("/upload/profile-image")
    public ResponseEntity<GenericResponseDTO> uploadProfileImage(@RequestParam MultipartFile file, @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        String key = s3Service.uploadSellerProfileImage(userPrincipal.getUsername(),file);
        return ResponseEntity.ok(new GenericResponseDTO(true,"Image uploaded at key : "+key));

    }

    @DeleteMapping("/profile/image")
    public ResponseEntity<GenericResponseDTO> deleteProfileImage(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        String username = userPrincipal.getUsername();
        boolean deleted = s3Service.deleteSellerProfileImage(username);
        if (deleted) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Profile image deleted successfully."));
        } else {
            return ResponseEntity.ok(new GenericResponseDTO(true, "No profile image found to delete."));
        }
    }


    @PutMapping("/profile")
    public ResponseEntity<GenericResponseDTO> updateSellerProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody SellerProfileUpdateDTO  sellerProfileUpdateDTO) {
        sellerService.updateSellerProfile(userPrincipal, sellerProfileUpdateDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, "Profile updated successfully."));
    }

    @PutMapping("/address/{addressId}")
    public ResponseEntity<GenericResponseDTO> updateSellerAddress(@PathVariable String addressId, @RequestBody AddressDTO addressDTO) {
        sellerService.updateSellerAddress(addressId,addressDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, "Address updated successfully."));
    }




}
