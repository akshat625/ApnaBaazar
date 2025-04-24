package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.InvalidSellerException;
import com.apnabaazar.apnabaazar.exceptions.ProductVariationNotFoundException;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationUpdateDTO;
import com.apnabaazar.apnabaazar.model.products.Product;
import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.ProductRepository;
import com.apnabaazar.apnabaazar.repository.ProductVariationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductVariationService {

    private final ProductRepository productRepository;
    private final ProductVariationRepository productVariationRepository;
    private final UserService userService;
    private final S3Service s3Service;
    private final ProductService productService;
    private final MessageSource messageSource;


    ProductVariation getProductVariationById(String variationId) {
        Locale locale = LocaleContextHolder.getLocale();
        return productVariationRepository.findById(variationId)
                .orElseThrow(() -> new ProductVariationNotFoundException(
                        messageSource.getMessage("product.variation.not.found", new Object[]{variationId}, locale)));
    }

    void validateSellerOwnership(Seller seller, Product product, Locale locale) {
        if (product.getSeller() != seller) {
            throw new InvalidSellerException(
                    messageSource.getMessage("seller.not.associated.with.product", new Object[]{product.getName()}, locale));
        }
    }

    void updateVariationDetails(ProductVariationUpdateDTO dto, ProductVariation variation, Product product, Locale locale) {
        if (dto == null) return;

        if (dto.getQuantity() != null)
            variation.setQuantityAvailable(dto.getQuantity());

        if (dto.getPrice() != null)
            variation.setPrice(dto.getPrice());

        if (dto.getMetadata() != null && !dto.getMetadata().isEmpty()) {
            productService.validateMetadata(dto.getMetadata(), product.getCategory(), locale, product);
            variation.setMetadata(dto.getMetadata());
        }
    }

    void updateImages(Product product, ProductVariation variation, MultipartFile primaryImage, List<MultipartFile> secondaryImages) {
        try {
            updatePrimaryImage(product, variation, primaryImage);
            uploadSecondaryImages(product.getId(), variation.getProductVariationId(), secondaryImages);
        } catch (IOException e) {
            log.error("Error updating images: {}", e.getMessage());
            throw new RuntimeException("Failed to update images: " + e.getMessage());
        }
    }

    void updatePrimaryImage(Product product, ProductVariation variation, MultipartFile primaryImage) throws IOException {
        if (primaryImage != null && !primaryImage.isEmpty()) {
            if (variation.getPrimaryImageName() != null) {
                s3Service.deleteObject(variation.getPrimaryImageName());
            }

            String imageKey = s3Service.uploadProductVariationImage(product.getId(), variation.getProductVariationId(), primaryImage, true);
            variation.setPrimaryImageName(imageKey);
        }
    }

    void uploadSecondaryImages(String productId, String variationId, List<MultipartFile> secondaryImages) throws IOException {
        if (secondaryImages == null || secondaryImages.isEmpty()) return;

        for (MultipartFile image : secondaryImages) {
            if (image != null && !image.isEmpty()) {
                s3Service.uploadProductVariationImage(productId, variationId, image, false);
            }
        }
    }

     ProductVariationResponseDTO mapToProductVariationResponseDTO(ProductVariation variation, String productId) {
        String imageUrl;
        try {
            imageUrl = s3Service.getObjectUrl(
                    "products/" + productId + "/variations/" + variation.getProductVariationId() +
                            s3Service.getExtension(variation.getPrimaryImageName())
            );
        } catch (IOException e) {
            imageUrl = "https://your-cdn.com/default-image.jpg"; // fallback image
        }

        return ProductVariationResponseDTO.builder()
                .active(variation.isActive())
                .metadata(variation.getMetadata())
                .quantity(variation.getQuantityAvailable())
                .price(variation.getPrice())
                .primaryImageUrl(imageUrl)
                .build();
    }


    ProductVariationDTO mapToProductVariationDTO(ProductVariation productVariation, Product product) {
        ProductVariationDTO dto = new ProductVariationDTO();
        dto.setProductId(product.getId());
        dto.setPrice(productVariation.getPrice());
        dto.setMetadata(productVariation.getMetadata());
        dto.setQuantity(productVariation.getQuantityAvailable());
        dto.setProductName(product.getName());
        dto.setBrand(product.getBrand());
        dto.setDescription(product.getDescription());
        return dto;
    }


}
