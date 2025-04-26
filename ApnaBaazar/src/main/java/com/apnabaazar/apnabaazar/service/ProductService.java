package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.DuplicateProductException;
import com.apnabaazar.apnabaazar.exceptions.InvalidProductStateException;
import com.apnabaazar.apnabaazar.exceptions.InvalidSellerException;
import com.apnabaazar.apnabaazar.exceptions.ProductNotFoundException;
import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataFieldValues;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationResponseDTO;
import com.apnabaazar.apnabaazar.model.products.Product;
import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.CategoryMetadataFieldValuesRepository;
import com.apnabaazar.apnabaazar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryMetadataFieldValuesRepository categoryMetadataFieldValuesRepository;
    private final S3Service s3Service;
    private final MessageSource messageSource;

    public Product getProductById(String productId) {
        log.debug("Fetching product with ID: {}", productId);
        Locale locale = LocaleContextHolder.getLocale();
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    String msg = messageSource.getMessage("product.not.found", new Object[]{productId}, locale);
                    log.warn("Product not found: {}", msg);
                    return new ProductNotFoundException(msg);
                });
    }

    public ProductResponseDTO getProduct(String productId, boolean skipValidations) {
        log.debug("Getting product with ID: {}, skipValidations: {}", productId, skipValidations);
        Product product = getProductById(productId);
        if (!skipValidations){
            validateDeleteAndActiveState(product);
            validateProductHasVariations(product);
        }
        return buildProductResponseDTO(product);
    }

    void validateDeleteState(Product product) {
        log.debug("Validating delete state for product ID: {}", product.getId());
        if (product.isDeleted()) {
            log.warn("Product is marked as deleted");
            throw new InvalidProductStateException(messageSource.getMessage("product.deleted", null, LocaleContextHolder.getLocale()));
        }
    }

    void validateDeleteAndActiveState(Product product) {
        log.debug("Validating delete and active state for product ID: {}", product.getId());
        Locale locale = LocaleContextHolder.getLocale();
        validateDeleteState(product);
        if (!product.isActive()) {
            log.warn("Product is inactive");
            throw new InvalidProductStateException(messageSource.getMessage("product.inactive", null, locale));
        }
    }

    private void validateProductHasVariations(Product product) {
        log.debug("Validating if product has variations for product ID: {}", product.getId());
        Locale locale = LocaleContextHolder.getLocale();
        if (product.getVariations() == null || product.getVariations().isEmpty())
            throw new InvalidProductStateException(messageSource.getMessage("product.variation.empty", new Object[]{product.getName()}, locale));
    }


    private ProductResponseDTO buildProductResponseDTO(Product product) {
        log.debug("Building ProductResponseDTO for product ID: {}", product.getId());
        ProductDTO productDTO = buildProductDTO(product);
        List<ProductVariationResponseDTO> variationDTOs = buildVariationDTOs(product);

        return ProductResponseDTO.builder()
                .sellerId(product.getSeller().getId())
                .product(productDTO)
                .productVariation(variationDTOs)
                .build();
    }

    ProductDTO buildProductDTO(Product product) {
        log.debug("Building ProductDTO for product ID: {}", product.getId());
        CategoryDTO categoryDTO = CategoryDTO.builder()
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getName())
                .build();

        return ProductDTO.builder()
                .productId(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .cancellable(product.isCancellable())
                .returnable(product.isReturnable())
                .active(product.isActive())
                .category(categoryDTO)
                .build();
    }

    private List<ProductVariationResponseDTO> buildVariationDTOs(Product product) {
        log.debug("Building variation DTOs for product ID: {}", product.getId());
        return product.getVariations().stream()
                .map(variation -> {
                    String primaryImageUrl = getPrimaryImageUrl(variation.getPrimaryImageName());
                    List<String> secondaryImageUrls = s3Service.getSecondaryImageUrls(
                            product.getId(), variation.getProductVariationId());

                    return ProductVariationResponseDTO.builder()
                            .metadata(variation.getMetadata())
                            .quantity(variation.getQuantityAvailable())
                            .price(variation.getPrice())
                            .active(variation.isActive())
                            .primaryImageUrl(primaryImageUrl)
                            .secondaryImageUrl(secondaryImageUrls)
                            .build();
                })
                .toList();
    }

    List<ProductResponseDTO> buildProductResponseDTOs(Pageable pageable, Specification<Product> spec, ProductRepository productRepository) {
        log.debug("Building product response DTOs with specification and pagination");
        Page<Product> productsPage = productRepository.findAll(spec, pageable);

        return productsPage.getContent().stream()
                .map(product -> {
                    ProductDTO productDTO = buildProductDTO(product);
                    List<ProductVariationResponseDTO> variationDTOs = buildVariationDTOs(product);

                    return ProductResponseDTO.builder()
                            .sellerId(product.getSeller().getId())
                            .product(productDTO)
                            .productVariation(variationDTOs)
                            .build();
                })
                .toList();
    }

    List<ProductDTO> getProductDTOS(Pageable pageable, Specification<Product> spec, ProductRepository productRepository) {
        log.debug("Fetching product DTOs with spec and pageable");
        Page<Product> productsPage = productRepository.findAll(spec, pageable);

        return productsPage.getContent().stream().map(product -> {
            CategoryDTO categoryDTO = CategoryDTO.builder()
                    .categoryId(product.getCategory().getCategoryId())
                    .categoryName(product.getCategory().getName())
                    .build();

            return ProductDTO.builder()
                    .productId(product.getId())
                    .sellerId(product.getSeller().getId())
                    .name(product.getName())
                    .brand(product.getBrand())
                    .description(product.getDescription())
                    .cancellable(product.isCancellable())
                    .returnable(product.isReturnable())
                    .active(product.isActive())
                    .category(categoryDTO)
                    .build();
        }).toList();
    }

    String getPrimaryImageUrl(String primaryImageName) {
        log.debug("Fetching primary image URL for image: {}", primaryImageName);
        if (primaryImageName != null && !primaryImageName.isEmpty()) {
            try {
                return s3Service.getObjectUrl(primaryImageName);
            } catch (IOException e) {
                log.error("Error getting primary image URL: {}", e.getMessage());
            }
        }
        return null;
    }

    void validateMetadata(Map<String, Object> metadata, Category category, Locale locale, Product product) {
        log.debug("Validating metadata for product ID: {}", product.getId());
        if (metadata == null || metadata.isEmpty()) {
            log.warn("Metadata is empty");
            throw new InvalidMetadataException(messageSource.getMessage("metadata.min.one", null, locale));
        }

        Set<String> newMetadataFields = metadata.keySet();

        if (!product.getVariations().isEmpty()) {
            Optional<ProductVariation> existingVariation = product.getVariations().stream()
                    .filter(v -> v.getMetadata() != null && !v.getMetadata().isEmpty())
                    .findFirst();

            if (existingVariation.isPresent()) {
                Set<String> existingFields = existingVariation.get().getMetadata().keySet();

                if (!existingFields.equals(newMetadataFields)) {
                    log.warn("Metadata structure mismatch");
                    throw new InvalidMetadataException(messageSource.getMessage("metadata.structure.mismatch", null, locale));
                }
            }
        }

        Map<String, Set<String>> existingMetadata = new HashMap<>();
        Category current = category;

        while (current != null) {
            List<CategoryMetadataFieldValues> metadataValues = categoryMetadataFieldValuesRepository.findByCategory(current);
            for (CategoryMetadataFieldValues value : metadataValues) {
                String fieldName = value.getCategoryMetadataField().getName();
                Set<String> allowedValues = Arrays.stream(value.getValues().split(",")).map(String::trim).collect(Collectors.toSet());
                existingMetadata.put(fieldName, allowedValues);
            }
            current = current.getParentCategory();
        }

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String fieldName = entry.getKey();
            String value = entry.getValue().toString();

            if (!existingMetadata.containsKey(fieldName)) {
                log.warn("Invalid metadata field: {}", fieldName);
                throw new InvalidMetadataException(messageSource.getMessage("metadata.field.invalid", new Object[]{fieldName}, locale));
            }
            if (!existingMetadata.get(fieldName).contains(value)) {
                log.warn("Invalid metadata value: {} for field: {}", value, fieldName);
                throw new InvalidMetadataException(messageSource.getMessage("metadata.value.invalid", new Object[]{value, fieldName}, locale));
            }
        }
    }

    void validateSellerOwnership(Product product, Seller seller) {
        log.debug("Validating seller ownership for product ID: {}", product.getId());
        Locale locale = LocaleContextHolder.getLocale();
        if (product.getSeller() != seller) {
            log.warn("Seller not associated with product: {}", product.getName());
            throw new InvalidSellerException(
                    messageSource.getMessage("seller.not.associated.with.product",
                            new Object[]{product.getName()}, locale));
        }
    }

    void updateProductNameIfValid(ProductUpdateDTO dto, Product product, String productId, Seller seller, Locale locale) {
        log.debug("Checking if product name needs update for product ID: {}", productId);
        if (dto.getName() != null && !dto.getName().isBlank() && !dto.getName().trim().replaceAll("\\s{2,}", " ").equals(product.getName())){
            boolean isDuplicate = productRepository.findAll().stream()
                    .filter(p -> !p.getId().equals(productId))
                    .filter(p -> p.getSeller().equals(seller))
                    .filter(p -> p.getCategory().equals(product.getCategory()))
                    .filter(p -> p.getBrand().equals(product.getBrand()))
                    .anyMatch(p -> p.getName().equals(dto.getName().trim().replaceAll("\\s{2,}", " ")));

            if (isDuplicate) {
                log.warn("Duplicate product name found: {}", dto.getName().replaceAll("\\s{2,}", " "));
                throw new DuplicateProductException(messageSource.getMessage(
                        "product.duplicate.name", new Object[]{dto.getName().trim().replaceAll("\\s{2,}", " "), product.getBrand().trim().replaceAll("\\s{2,}", " ")
                        }, locale));
            }
            product.setName(dto.getName().trim().replaceAll("\\s{2,}", " "));
        }
    }

    void checkForDuplicateProduct(ProductDTO productDTO, Seller seller, Category category, Locale locale) {
        log.debug("Checking for duplicate product: {} by {}", productDTO.getName(), seller.getId());
        boolean isDuplicate = productRepository.existsByNameAndBrandAndCategoryAndSeller(productDTO.getName().trim().replaceAll("\\s{2,}", " "), productDTO.getBrand().trim().replaceAll("\\s{2,}", " "), category, seller);
        if (isDuplicate) {
            log.warn("Duplicate product found: {} - {}", productDTO.getName(), productDTO.getBrand());
            throw new DuplicateProductException(messageSource.getMessage("product.duplicate.name",
                    new Object[]{productDTO.getName().trim().replaceAll("\\s{2,}", " "), productDTO.getBrand().trim().replaceAll("\\s{2,}", " ")}, locale));
        }
    }

    Product buildProductFromDTO(ProductDTO productDTO, Seller seller, Category category) {
        log.debug("Building product entity from DTO for seller ID: {}", seller.getId());
        return Product.builder()
                .name(productDTO.getName().trim().replaceAll("\\s{2,}", " "))
                .brand(productDTO.getBrand().trim().replaceAll("\\s{2,}", " "))
                .category(category)
                .seller(seller)
                .cancellable(productDTO.isCancellable())
                .returnable(productDTO.isReturnable())
                .description(productDTO.getDescription().trim().replaceAll("\\s{2,}", " "))
                .build();
    }
}
