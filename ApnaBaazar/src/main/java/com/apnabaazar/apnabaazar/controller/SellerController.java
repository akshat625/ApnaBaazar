package com.apnabaazar.apnabaazar.controller;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CustomerCategoryResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.ProfileUpdateDTO;
import com.apnabaazar.apnabaazar.service.CustomerService;
import com.apnabaazar.apnabaazar.service.S3Service;
import com.apnabaazar.apnabaazar.service.SellerService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@RestController

@RequestMapping("/seller")
public class SellerController {

    private final SellerService sellerService;
    private final S3Service s3Service;
    private final MessageSource messageSource;
    private final CustomerService customerService;
    private Locale locale;

    @ModelAttribute
    public void initLocale() {
        this.locale = LocaleContextHolder.getLocale();
    }

    @GetMapping("/test")
    public String testCustomer() {
        return messageSource.getMessage("seller.hello.message", new Object[]{}, locale);
    }

    @GetMapping("/profile")
    public ResponseEntity<SellerProfileDTO> getSellerProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return sellerService.getSellerProfile(userPrincipal);
    }

    @PostMapping("/upload/profile-image")
    public ResponseEntity<GenericResponseDTO> uploadSellerProfileImage(@RequestParam MultipartFile file, @AuthenticationPrincipal UserPrincipal userPrincipal) throws IOException {
        String key = s3Service.uploadProfileImage(userPrincipal.getUsername(), file);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("image.uploaded", new Object[]{key}, locale) + key));


    }

    @DeleteMapping("/profile/image")
    public ResponseEntity<GenericResponseDTO> deleteSellerProfileImage(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        String username = userPrincipal.getUsername();
        boolean deleted = s3Service.deleteProfileImage(username);
        String messageKey = deleted ? "image.deleted" : "image.not-found";
        String message = messageSource.getMessage(messageKey, null, locale);
        return ResponseEntity.ok(new GenericResponseDTO(true, message));
    }


    @PutMapping("/profile")
    public ResponseEntity<GenericResponseDTO> updateSellerProfile(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody ProfileUpdateDTO sellerProfileUpdateDTO) {
        sellerService.updateSellerProfile(userPrincipal, sellerProfileUpdateDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("profile.updated", null, locale)));
    }

    @PutMapping("/address/{addressId}")
    public ResponseEntity<GenericResponseDTO> updateSellerAddress(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String addressId, @Valid @RequestBody AddressUpdateDTO addressUpdateDTO) {
        sellerService.updateSellerAddress(userPrincipal, addressId, addressUpdateDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("address.updated", null, locale)));
    }

    @PutMapping("/password")
    public ResponseEntity<GenericResponseDTO> updateSellerPassword(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        sellerService.updateSellerPassword(userPrincipal, updatePasswordDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("password.updated", null, locale)));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return ResponseEntity.ok(sellerService.getAllCategories());
    }

    //------------------------------------------------------------------------------------------------

    @PostMapping("/product")
    public ResponseEntity<GenericResponseDTO> addProduct(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody ProductDTO productDTO) throws MessagingException {
        sellerService.addProduct(userPrincipal, productDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("product.added.success", null, locale)));
    }

    @PutMapping("/product/{productId}")
    public ResponseEntity<GenericResponseDTO> updateProduct(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody ProductUpdateDTO productDTO, String productId)  {
        sellerService.updateProduct(userPrincipal, productDTO, productId);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("product.updated.success", null, locale)));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ProductDTO> getProduct(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String productId)  {
        return ResponseEntity.ok(sellerService.getProduct(userPrincipal,productId));
    }

    @PostMapping("/product/variations")
    public ResponseEntity<String> addProductVariation(
            @RequestPart("productData") @Valid ProductVariationDTO dto,
            @RequestPart("primaryImage") MultipartFile primaryImage,
            @RequestPart(value = "secondaryImages", required = false) List<MultipartFile> secondaryImages
    ){
        sellerService.addProductVariations(dto,primaryImage,secondaryImages);
        return ResponseEntity.ok(messageSource.getMessage("product.variation.added.success", null, locale));
    }

    @GetMapping("/product/variations/{variationId}")
    public ResponseEntity<ProductVariationDTO> getProductVariation(@AuthenticationPrincipal UserPrincipal userPrincipal,@PathVariable String variationId) {
        return ResponseEntity.ok(sellerService.getProductVariation(userPrincipal,variationId));
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<GenericResponseDTO> deleteProduct(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable String productId) {
        sellerService.deleteProduct(userPrincipal,productId);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("product.deleted.success", null, locale)));
    }

    @PutMapping("/product/variations/{variationId}")
    public ResponseEntity<GenericResponseDTO> updateProductVariation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String variationId,
            @RequestPart(value = "productData", required = false) ProductVariationUpdateDTO dto,
            @RequestPart(value = "primaryImage", required = false) MultipartFile primaryImage,
            @RequestPart(value = "secondaryImages", required = false) List<MultipartFile> secondaryImages
    ) {
        sellerService.updateProductVariation(userPrincipal, variationId, dto, primaryImage, secondaryImages);
        return ResponseEntity.ok(new GenericResponseDTO(true,
                messageSource.getMessage("product.variation.updated.success", null, locale)));
    }



}
