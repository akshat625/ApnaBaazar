package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.model.dto.*;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerDTO;
import com.apnabaazar.apnabaazar.service.AuthService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Locale;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final MessageSource messageSource;
    private Locale locale;

    @ModelAttribute
    public void initLocale() {
        this.locale = LocaleContextHolder.getLocale();
    }


    @PostMapping("/register/seller")
    public ResponseEntity<GenericResponseDTO> registerSeller(@Valid @RequestBody SellerDTO sellerDTO) {
        authService.sellerSignup(sellerDTO);
        return new ResponseEntity<>(new GenericResponseDTO(true, messageSource.getMessage("seller.register.success",null,locale)),HttpStatus.CREATED);
    }

    @PostMapping("/register/customer")
    public ResponseEntity<GenericResponseDTO> registerCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        authService.customerSignup(customerDTO);
        return new ResponseEntity<>(new GenericResponseDTO(true, messageSource.getMessage("customer.register.success",null,locale)),HttpStatus.CREATED);
    }

    @PutMapping("/verify/{token}")
    public String activateCustomer(@PathVariable String token) {
        return authService.verifyUser(token);
    }

    @PostMapping("/resend/{emailId}")
    public String resendVerificationEmail(@PathVariable String emailId) throws MessagingException {
        return authService.resendVerificationEmail(emailId);
    }

    @PostMapping("/login/customer")
    public ResponseEntity<LoginResponseDTO> loginCustomer(@RequestBody LoginDTO loginDTO) throws AccessDeniedException {
        return new ResponseEntity<>(authService.login(loginDTO, "ROLE_CUSTOMER"), HttpStatus.OK);
    }

    @PostMapping("/login/seller")
    public ResponseEntity<LoginResponseDTO> loginSeller(@RequestBody LoginDTO loginDTO) throws AccessDeniedException {
        return new ResponseEntity<>(authService.login(loginDTO, "ROLE_SELLER"), HttpStatus.OK);
    }

    @PostMapping("/login/admin")
    public ResponseEntity<LoginResponseDTO> loginAdmin(@Valid @RequestBody LoginDTO loginDTO) throws AccessDeniedException {
        return new ResponseEntity<>(authService.login(loginDTO, "ROLE_ADMIN"), HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordDTO  forgotPasswordDTO) {
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

    @PostMapping("/logout/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> logoutAdmin(@RequestParam String token) {
        return new ResponseEntity<>(authService.logout(token), HttpStatus.OK);
    }

    @PostMapping("/logout/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> logoutCustomer(@RequestParam String token) {
        return new ResponseEntity<>(authService.logout(token), HttpStatus.OK);
    }

    @PostMapping("/logout/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> logoutSeller(@RequestParam String token) {
        return new ResponseEntity<>(authService.logout(token), HttpStatus.OK);
    }

}
