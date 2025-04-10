package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.model.dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.service.S3Service;
import com.apnabaazar.apnabaazar.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<String> uploadProfileImage(@RequestParam MultipartFile file, @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        String key = s3Service.uploadSellerProfileImage(userPrincipal.getUsername(),file);
        return ResponseEntity.ok("Image uploaded at key : "+key);
    }

    @GetMapping("/profile-image")
    public ResponseEntity<byte[]> downloadSellerImage(@RequestParam String extension,
                                                      @AuthenticationPrincipal UserDetails user) {
        byte[] image = s3Service.downloadSellerProfileImage(user.getUsername(), extension);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(image);
    }
}
