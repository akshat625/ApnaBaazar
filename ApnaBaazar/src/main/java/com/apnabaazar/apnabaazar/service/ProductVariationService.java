package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.InvalidImageFormatException;
import com.apnabaazar.apnabaazar.exceptions.InvalidSellerException;
import com.apnabaazar.apnabaazar.exceptions.ProductVariationNotFoundException;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationUpdateDTO;
import com.apnabaazar.apnabaazar.model.products.Product;
import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import com.apnabaazar.apnabaazar.model.users.Seller;
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

    private final ProductVariationRepository productVariationRepository;
    private final S3Service s3Service;
    private final ProductService productService;
    private final MessageSource messageSource;

    ProductVariation getProductVariationById(String variationId) {
        Locale locale = LocaleContextHolder.getLocale();
        log.debug("Fetching product variation with ID: {}", variationId);
        return productVariationRepository.findById(variationId)
                .orElseThrow(() -> {
                    log.error("Product variation not found with ID: {}", variationId);
                    return new ProductVariationNotFoundException(
                            messageSource.getMessage("product.variation.not.found", new Object[]{variationId}, locale));
                });
    }

    void updateVariationDetails(ProductVariationUpdateDTO dto, ProductVariation variation, Product product, Locale locale) {
        log.debug("Updating variation details for variation ID: {}", variation.getProductVariationId());
        if (dto == null) return;

        if (dto.getQuantity() != null) {
            log.debug("Setting quantity to {}", dto.getQuantity());
            variation.setQuantityAvailable(dto.getQuantity());
        }
        if (dto.getPrice() != null) {
            log.debug("Setting price to {}", dto.getPrice());
            variation.setPrice(dto.getPrice());
        }
        if (dto.getMetadata() != null && !dto.getMetadata().isEmpty()) {
            log.debug("Validating and setting metadata for variation ID: {}", variation.getProductVariationId());
            productService.validateMetadata(dto.getMetadata(), product.getCategory(), locale, product);
            variation.setMetadata(dto.getMetadata());
        }
    }

    void updateImages(Product product, ProductVariation variation, MultipartFile primaryImage, List<MultipartFile> secondaryImages) {
        try {
            log.info("Updating images for product ID: {}, variation ID: {}", product.getId(), variation.getProductVariationId());
            updatePrimaryImage(product, variation, primaryImage);
            uploadSecondaryImages(product.getId(), variation.getProductVariationId(), secondaryImages);
        } catch (IOException e) {
            log.error("Error updating images for variation ID {}: {}", variation.getProductVariationId(), e.getMessage(), e);
            throw new InvalidImageFormatException("Failed to update images: " + e.getMessage());
        }
    }

    void updatePrimaryImage(Product product, ProductVariation variation, MultipartFile primaryImage) throws IOException {
        if (primaryImage != null && !primaryImage.isEmpty()) {
            log.debug("Updating primary image for variation ID: {}", variation.getProductVariationId());

            if (variation.getPrimaryImageName() != null) {
                log.debug("Deleting old primary image: {}", variation.getPrimaryImageName());
                s3Service.deleteObject(variation.getPrimaryImageName());
            }

            String imageKey = s3Service.uploadProductVariationImage(product.getId(), variation.getProductVariationId(), primaryImage, true);
            log.debug("Uploaded new primary image: {}", imageKey);
            variation.setPrimaryImageName(imageKey);
        }
    }

    void uploadSecondaryImages(String productId, String variationId, List<MultipartFile> secondaryImages) throws IOException {
        if (secondaryImages == null || secondaryImages.isEmpty()) {
            log.debug("No secondary images provided for variation ID: {}", variationId);
            return;
        }

        for (MultipartFile image : secondaryImages) {
            if (image != null && !image.isEmpty()) {
                log.debug("Uploading secondary image for variation ID: {}", variationId);
                s3Service.uploadProductVariationImage(productId, variationId, image, false);
            }
        }
    }

    ProductVariationResponseDTO mapToProductVariationResponseDTO(ProductVariation variation, String productId) {
        log.debug("Mapping variation ID: {} to response DTO", variation.getProductVariationId());
        String imageUrl;
        try {
            imageUrl = s3Service.getObjectUrl(
                    "products/" + productId + "/variations/" + variation.getProductVariationId() +
                            s3Service.getExtension(variation.getPrimaryImageName())
            );
            log.debug("Fetched primary image URL: {}", imageUrl);
        } catch (IOException e) {
            imageUrl = "https://your-cdn.com/default-image.jpg";
            log.warn("Failed to get image URL for variation ID: {}, using fallback", variation.getProductVariationId(), e);
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
        log.debug("Mapping product variation ID: {} to admin DTO", productVariation.getProductVariationId());
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
