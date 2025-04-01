package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.model.dto.CustomerDTO;
import com.apnabaazar.apnabaazar.model.dto.LoginDTO;
import com.apnabaazar.apnabaazar.model.dto.SellerDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {


    @PostMapping("/register/seller")
    public ResponseEntity<String> registerSeller(@RequestBody SellerDTO sellerDTO) {
        return ResponseEntity.ok("Seller registered successfully!");
    }

    @PostMapping("/register/customer")
    public ResponseEntity<String> registerCustomer(@RequestBody CustomerDTO customerDTO) {
        return ResponseEntity.ok("Customer registered successfully!");
    }

    @PostMapping("/login/customer")
    public ResponseEntity<String> loginCustomer(@RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok("Customer logged in successfully!");
    }

    @PostMapping("/login/seller")
    public ResponseEntity<String> loginSeller(@RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok("Seller logged in successfully!");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        return ResponseEntity.ok("Password reset instructions sent to email!");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        return ResponseEntity.ok("Password successfully reset!");
    }
}
