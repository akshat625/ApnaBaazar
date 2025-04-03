package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.model.dto.CustomerDTO;
import com.apnabaazar.apnabaazar.model.dto.LoginDTO;
import com.apnabaazar.apnabaazar.model.dto.SellerDTO;
import com.apnabaazar.apnabaazar.service.CustomerService;
import com.apnabaazar.apnabaazar.service.SellerService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.RoleNotFoundException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SellerService sellerService;

    @PostMapping("/register/seller")
    public ResponseEntity<String> registerSeller(@RequestBody SellerDTO sellerDTO) throws MessagingException {
        sellerService.sellerSignup(sellerDTO);
        return ResponseEntity.ok("Seller registered successfully!");
    }

    @PostMapping("/register/customer")
    public ResponseEntity<String> registerCustomer(@RequestBody CustomerDTO customerDTO) throws MessagingException, RoleNotFoundException {
        customerService.customerSignup
                (customerDTO);
        return ResponseEntity.ok("Customer registered successfully!");
    }

    @PutMapping("/verify/{token}")
    public String activateCustomer(@PathVariable String token) throws MessagingException {
        return customerService.verifyUser(token);
    }

    @PostMapping("/resend/{emailId}")
    public String resendVerificationEmail(@PathVariable String emailId) throws MessagingException {
        return customerService.resendVerificationEmail(emailId);
    }

    @PostMapping("/login/customer")
    public ResponseEntity<String> loginCustomer(@RequestBody LoginDTO loginDTO) {
        return new ResponseEntity<>(customerService.login(loginDTO), HttpStatus.OK);
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
