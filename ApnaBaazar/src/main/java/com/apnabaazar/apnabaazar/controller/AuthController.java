package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.model.dto.*;
import com.apnabaazar.apnabaazar.service.AuthService;
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
    private AuthService authService;

    @PostMapping("/register/seller")
    public ResponseEntity<String> registerSeller(@RequestBody SellerDTO sellerDTO) throws MessagingException, RoleNotFoundException {
        authService.sellerSignup(sellerDTO);
        return ResponseEntity.ok("Seller registered successfully!");
    }

    @PostMapping("/register/customer")
    public ResponseEntity<String> registerCustomer(@RequestBody  CustomerDTO customerDTO) throws MessagingException, RoleNotFoundException {
        authService.customerSignup
                (customerDTO);
        return ResponseEntity.ok("Customer registered successfully!");
    }

    @PutMapping("/verify/{token}")
    public String activateCustomer(@PathVariable String token) throws MessagingException {
        return authService.verifyUser(token);
    }

    @PostMapping("/resend/{emailId}")
    public String resendVerificationEmail(@PathVariable String emailId) throws MessagingException, RoleNotFoundException {
        return authService.resendVerificationEmail(emailId);
    }

    @PostMapping("/login/customer"  )
    public ResponseEntity<LoginResponseDTO> loginCustomer(@RequestBody LoginDTO loginDTO) throws MessagingException {
        return new ResponseEntity<>(authService.login(loginDTO), HttpStatus.OK);
    }

    @PostMapping("/login/seller")
    public ResponseEntity<LoginResponseDTO> loginSeller(@RequestBody LoginDTO loginDTO) throws MessagingException {
        return new ResponseEntity<>(authService.login(loginDTO), HttpStatus.OK);
    }

    @PostMapping("/login/admin")
    public ResponseEntity<LoginResponseDTO> loginAdmin(@RequestBody LoginDTO loginDTO) throws MessagingException {
        return new ResponseEntity<>(authService.login(loginDTO), HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordDTO  forgotPasswordDTO) throws MessagingException {

        return ResponseEntity.ok(authService.forgotPassword(forgotPasswordDTO));
    }

    @PutMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {

        return ResponseEntity.ok(authService.resetPassword(resetPasswordDTO));
    }


    @PostMapping("/refresh-token/{refreshToken}")
    public ResponseEntity<LoginResponseDTO> refreshToken(@PathVariable String refreshToken) {
        LoginResponseDTO response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam String token) {
        return new ResponseEntity<>(authService.logout(token), HttpStatus.OK);

    }
}
