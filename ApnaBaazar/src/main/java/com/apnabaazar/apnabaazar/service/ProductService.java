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
import com.apnabaazar.apnabaazar.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        Locale locale = LocaleContextHolder.getLocale();
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        messageSource.getMessage("product.not.found", new Object[]{productId}, locale)
                ));
    }


    public ProductResponseDTO getProduct(String productId, boolean skipValidations) {
        Product product = getProductById(productId);
        if (!skipValidations){
            validateDeleteAndActiveState(product);
            validateProductHasVariations(product);
        }
        return buildProductResponseDTO(product);
    }

    void validateDeleteState(Product product) {
        if (product.isDeleted()) {
            throw new InvalidProductStateException(messageSource.getMessage("product.deleted", null, LocaleContextHolder.getLocale()));
        }
    }
     void validateDeleteAndActiveState(Product product) {
        Locale locale = LocaleContextHolder.getLocale();
        validateDeleteState(product);
        if (!product.isActive()) {
            throw new InvalidProductStateException(messageSource.getMessage("product.inactive", null, locale));
        }
    }

    private void validateProductHasVariations(Product product) {
        Locale locale = LocaleContextHolder.getLocale();
        if (product.getVariations() == null || product.getVariations().isEmpty())
            throw new InvalidProductStateException(messageSource.getMessage("product.variation.empty", new Object[]{product.getName()}, locale));
    }


    private ProductResponseDTO buildProductResponseDTO(Product product) {
        ProductDTO productDTO = buildProductDTO(product);
        List<ProductVariationResponseDTO> variationDTOs = buildVariationDTOs(product);

        return ProductResponseDTO.builder()
                .sellerId(product.getSeller().getId())
                .product(productDTO)
                .productVariation(variationDTOs)
                .build();
    }

     ProductDTO buildProductDTO(Product product) {
        CategoryDTO categoryDTO = CategoryDTO.builder()
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getName())
                .build();

        return ProductDTO.builder()
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
                .collect(Collectors.toList());
    }

    String getPrimaryImageUrl(String primaryImageName) {
        if (primaryImageName != null && !primaryImageName.isEmpty()) {
            try {
                return s3Service.getObjectUrl(primaryImageName);
            } catch (IOException e) {
                log.error("Error getting primary image URL: {}", e.getMessage());
            }
        }
        return null;
    }


     List<ProductDTO> getProductDTOS(Pageable pageable, Specification<Product> spec, ProductRepository productRepository) {
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


    void validateMetadata(Map<String, Object> metadata, Category category, Locale locale, Product product) {
        if (metadata == null || metadata.isEmpty()) {
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

            if (!existingMetadata.containsKey(fieldName))
                throw new InvalidMetadataException(messageSource.getMessage("metadata.field.invalid", new Object[]{fieldName}, locale));
            if (!existingMetadata.get(fieldName).contains(value))
                throw new InvalidMetadataException(messageSource.getMessage("metadata.value.invalid", new Object[]{value, fieldName}, locale));
        }
    }

    void validateSellerOwnership(Product product, Seller seller) {
        Locale locale = LocaleContextHolder.getLocale();
        if (product.getSeller() != seller) {
            throw new InvalidSellerException(
                    messageSource.getMessage("seller.not.associated.with.product",
                            new Object[]{product.getName()}, locale));
        }
    }

    void updateProductNameIfValid(ProductUpdateDTO dto, Product product, String productId, Seller seller, Locale locale) {
        if (dto.getName() != null && !dto.getName().isBlank() && !dto.getName().equals(product.getName())) {
            boolean isDuplicate = productRepository.findAll().stream()
                    .filter(p -> !p.getId().equals(productId))
                    .filter(p -> p.getSeller().equals(seller))
                    .filter(p -> p.getCategory().equals(product.getCategory()))
                    .filter(p -> p.getBrand().equals(product.getBrand()))
                    .anyMatch(p -> p.getName().equals(dto.getName()));

            if (isDuplicate) {
                throw new DuplicateProductException(messageSource.getMessage(
                        "product.duplicate.name", new Object[]{dto.getName(), product.getBrand()}, locale));
            }
            product.setName(dto.getName());
        }
    }

    void checkForDuplicateProduct(ProductDTO productDTO, Seller seller, Category category, Locale locale) {
        boolean isDuplicate = productRepository.existsByNameAndBrandAndCategoryAndSeller(productDTO.getName(), productDTO.getBrand(), category, seller);
        if (isDuplicate) {
            throw new DuplicateProductException(messageSource.getMessage("product.duplicate.name",
                    new Object[]{productDTO.getName(), productDTO.getBrand()}, locale));
        }
    }

    Product buildProductFromDTO(ProductDTO productDTO, Seller seller, Category category) {
        return Product.builder()
                .name(productDTO.getName())
                .brand(productDTO.getBrand())
                .category(category)
                .seller(seller)
                .cancellable(productDTO.isCancellable())
                .returnable(productDTO.isReturnable())
                .description(productDTO.getDescription())
                .build();
    }




}