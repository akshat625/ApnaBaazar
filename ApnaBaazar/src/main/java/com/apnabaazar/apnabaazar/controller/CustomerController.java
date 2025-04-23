package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CustomerCategoryResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.ProfileUpdateDTO;
import com.apnabaazar.apnabaazar.service.CustomerService;
import com.apnabaazar.apnabaazar.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;
    private final S3Service s3Service;
    private final MessageSource messageSource;
    private Locale locale;

    @ModelAttribute
    public void initLocale() {
        this.locale = LocaleContextHolder.getLocale();
    }

    @GetMapping("/hello")
    public String testCustomer() {
        return messageSource.getMessage("customer.hello.message", new Object[]{},locale);
    }

    @GetMapping("/profile")
    public ResponseEntity<CustomerProfileDTO> getCustomerProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return customerService.getCustomerProfile(userPrincipal);
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getCustomerAddresses(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return customerService.getCustomerAddresses(userPrincipal);
    }

    @PostMapping("/address")
    public ResponseEntity<GenericResponseDTO> addCustomerAddress(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody AddressDTO  addressDTO) {
        customerService.addCustomerAddress(userPrincipal,addressDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("address.added", null, locale)));
    }

    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<GenericResponseDTO> deleteCustomerAddress(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String addressId) throws AccessDeniedException {
        customerService.deleteCustomerAddress(userPrincipal, addressId);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("address.deleted", null, locale)));
    }


    @PostMapping("/upload/profile-image")
    public ResponseEntity<GenericResponseDTO> uploadCustomerProfileImage(@RequestParam MultipartFile file, @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        String key = s3Service.uploadProfileImage(userPrincipal.getUsername(), file);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("image.uploaded", new Object[]{key},locale) + key));

    }

    @DeleteMapping("/profile/image")
    public ResponseEntity<GenericResponseDTO> deleteCustomerProfileImage(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        String username = userPrincipal.getUsername();
        boolean deleted = s3Service.deleteProfileImage(username);
        String messageKey = deleted ? "image.deleted" : "image.not-found";
        String message = messageSource.getMessage(messageKey, null, locale);
        return ResponseEntity.ok(new GenericResponseDTO(true, message));
    }

    @PutMapping("/profile")
    public ResponseEntity<GenericResponseDTO> updateSellerProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody ProfileUpdateDTO customerProfileUpdateDTO) {
        customerService.updateCustomerProfile(userPrincipal, customerProfileUpdateDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("profile.updated", null,locale)));
    }


    @PutMapping("/address/{addressId}")
    public ResponseEntity<GenericResponseDTO> updateCustomerAddress(UserPrincipal userPrincipal, @PathVariable String addressId, @Valid @RequestBody AddressUpdateDTO addressUpdateDTO) throws AccessDeniedException {
        customerService.updateCustomerAddress(userPrincipal, addressId, addressUpdateDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("address.updated", null,locale)));
    }

    @PutMapping("/password")
    public ResponseEntity<GenericResponseDTO> updateCustomerPassword(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        customerService.updateCustomerPassword(userPrincipal, updatePasswordDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("password.updated", null,locale)));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CustomerCategoryResponseDTO>> getAllCategories(@RequestParam(required = false) String categoryId) {
        return ResponseEntity.ok(customerService.getAllCategories(categoryId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(customerService.getProduct(productId));
    }
}
