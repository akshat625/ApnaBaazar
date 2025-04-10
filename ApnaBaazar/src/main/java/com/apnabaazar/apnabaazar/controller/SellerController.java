package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.model.dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RequiredArgsConstructor
@RestController
@RequestMapping("/seller")
public class SellerController {

    private final SellerService sellerService;

    @GetMapping("/test")
    public String testCustomer(){
        return "Hello World! from Seller";
    }

    @GetMapping("/profile")
    public ResponseEntity<SellerProfileDTO>  getProfile(){
        return sellerService.getSellerProfile();
    }
}
