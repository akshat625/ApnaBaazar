package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataFieldValues;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryMetadataFieldValueDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.ProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.products.Product;
import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Role;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.*;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariationRepository productVariationRepository;
    private final CategoryMetadataFieldValuesRepository categoryMetadataFieldValuesRepository;
    private final S3Service s3Service;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MessageSource messageSource;
    private final EmailService emailService;
    private final RoleRepository roleRepository;


    @Value("${aws.s3.default-seller-image}")
    private String defaultSellerImage;

    public ResponseEntity<SellerProfileDTO> getSellerProfile(UserPrincipal userPrincipal) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        log.info("Fetching profile for seller: {}", email);

        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Seller not found with email: {}", email);
                    return new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale));
                });

        try {
            String imageUrl = s3Service.getProfileImageUrl(email, defaultSellerImage);
            log.info("Seller profile image URL resolved: {}", imageUrl);

            return ResponseEntity.ok(SellerMapper.toSellerProfileDTO(seller, imageUrl));
        } catch (Exception e) {
            log.error("Error retrieving seller profile for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getUpdatedValue(String newValue, String oldValue) {
        return (newValue != null && !newValue.isBlank()) ? newValue : oldValue;
    }


    public void updateSellerProfile(UserPrincipal userPrincipal, ProfileUpdateDTO sellerProfileUpdateDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        log.info("Updating profile for seller: {}", email);
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));
        if (sellerProfileUpdateDTO != null) {
            seller.setFirstName(getUpdatedValue(sellerProfileUpdateDTO.getFirstName(), seller.getFirstName()));
            seller.setMiddleName(getUpdatedValue(sellerProfileUpdateDTO.getMiddleName(), seller.getMiddleName()));
            seller.setLastName(getUpdatedValue(sellerProfileUpdateDTO.getLastName(), seller.getLastName()));
            seller.setCompanyContact(getUpdatedValue(sellerProfileUpdateDTO.getContact(), seller.getCompanyContact()));
        }
        sellerRepository.save(seller);
        log.info("Seller profile updated successfully for: {}", email);
    }

    public void updateSellerAddress(UserPrincipal userPrincipal, String addressId, AddressUpdateDTO addressUpdateDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("address.not.found", new Object[]{addressId}, locale)));
        log.info("Updating address [ID: {}] for seller: {}", addressId, email);

        if (addressUpdateDTO != null) {
            address.setAddressLine(getUpdatedValue(addressUpdateDTO.getAddressLine(), address.getAddressLine()));
            address.setCity(getUpdatedValue(addressUpdateDTO.getCity(), address.getCity()));
            address.setState(getUpdatedValue(addressUpdateDTO.getState(), address.getState()));
            address.setZipCode(getUpdatedValue(addressUpdateDTO.getZipCode(), address.getZipCode()));
            address.setCountry(getUpdatedValue(addressUpdateDTO.getCountry(), address.getCountry()));
        }
        addressRepository.save(address);
        log.info("Address [ID: {}] updated successfully for seller: {}", addressId, email);
    }

    public void updateSellerPassword(UserPrincipal userPrincipal, UpdatePasswordDTO updatePasswordDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        log.info("Updating seller password for seller: {}", email);
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), seller.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.old.incorrect", null, locale));
        if (passwordEncoder.matches(updatePasswordDTO.getNewPassword(), seller.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.new.password.incorrect", null, locale));
        if (!updatePasswordDTO.getNewPassword().equals(updatePasswordDTO.getConfirmPassword())) {
            throw new PasswordMismatchException(messageSource.getMessage("password.mismatch", null, locale));
        }

        seller.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        seller.setPasswordUpdateDate(LocalDateTime.now());
        sellerRepository.save(seller);

        log.info("Seller password updated successfully for: {}", email);
    }

    public List<CategoryResponseDTO> getAllCategories() {
        // Find all categories
        List<Category> allCategories = categoryRepository.findAll();

        // Filter to get only leaf categories (those without subcategories)
        List<Category> leafCategories = allCategories.stream()
                .filter(category -> category.getSubCategories() == null || category.getSubCategories().isEmpty())
                .collect(Collectors.toList());

        // Map leaf categories to response DTOs with full metadata and hierarchy
        List<CategoryResponseDTO> categoryResponseDTOs = leafCategories.stream()
                .map(this::buildCategoryResponseDTO)
                .collect(Collectors.toList());

        return categoryResponseDTOs;
    }

    private CategoryResponseDTO buildCategoryResponseDTO(Category category) {
        CategoryResponseDTO responseDTO = new CategoryResponseDTO();
        responseDTO.setCategoryId(category.getCategoryId());
        responseDTO.setName(category.getName());

        // Add parent hierarchy
        if (category.getParentCategory() != null) {
            responseDTO.setParentHierarchy(buildParentHierarchy(category));
        }

        // Collect metadata fields from this category and all parent categories
        List<CategoryMetadataFieldValueDTO> metadataDTOs = new ArrayList<>();
        Category current = category;
        while (current != null) {
            List<CategoryMetadataFieldValues> metadataValues = categoryMetadataFieldValuesRepository.findByCategory(current);
            if (metadataValues != null && !metadataValues.isEmpty()) {
                metadataDTOs.addAll(
                        metadataValues.stream()
                                .map(value -> {
                                    CategoryMetadataFieldValueDTO dto = new CategoryMetadataFieldValueDTO();
                                    dto.setFieldId(value.getCategoryMetadataField().getId());
                                    dto.setFieldName(value.getCategoryMetadataField().getName());
                                    dto.setValues(value.getValues());
                                    return dto;
                                })
                                .toList()
                );
            }
            current = current.getParentCategory();
        }
        responseDTO.setMetadataFields(metadataDTOs);

        return responseDTO;
    }

    private List<CategoryDTO> buildParentHierarchy(Category category) {
        List<CategoryDTO> hierarchy = new ArrayList<>();
        Category current = category.getParentCategory();

        while (current != null) {
            CategoryDTO parentDTO = new CategoryDTO();
            parentDTO.setCategoryName(current.getName());
            parentDTO.setCategoryId(current.getCategoryId());
            parentDTO.setParentId(current.getParentCategory() != null ?
                    current.getParentCategory().getCategoryId() : null);
            hierarchy.add(0, parentDTO); // Add at beginning to maintain root->leaf order
            current = current.getParentCategory();
        }
        return hierarchy;
    }

    public void addProduct(UserPrincipal userPrincipal, @Valid ProductDTO productDTO) throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(messageSource.getMessage("category.not.found", new Object[]{productDTO.getCategoryId()}, locale)));

        if (!category.getSubCategories().isEmpty())
            throw new InvalidLeafCategoryException(messageSource.getMessage("category.not.leaf", new Object[]{category.getName()}, locale));

        boolean isDuplicate = productRepository.existsByNameAndBrandAndCategoryAndSeller(productDTO.getName(), productDTO.getBrand(), category, seller);
        if (isDuplicate)
            throw new DuplicateProductException(messageSource.getMessage("product.duplicate.name", new Object[]{productDTO.getName(), productDTO.getBrand()}, locale));

        Product product = Product.builder()
                .name(productDTO.getName())
                .brand(productDTO.getBrand())
                .category(category)
                .seller(seller)
                .cancellable(productDTO.isCancellable())
                .returnable(productDTO.isReturnable())
                .description(productDTO.getDescription())
                .build();

        productRepository.save(product);


        Optional<Role> role = roleRepository.findByAuthority("ROLE_ADMIN");

        Set<Role> roles = new HashSet<>();
        roles.add(role.get());
        User admin = userRepository.findByRoles(roles);
        emailService.sendProductAddedMail(admin.getEmail(), "Product Added");

    }

    public ProductDTO getProduct(UserPrincipal userPrincipal, String productId) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));


        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(messageSource.getMessage("product.not.found", new Object[]{productId}, locale)));

        if (product.getSeller() != seller)
            throw new InvalidSellerException(messageSource.getMessage("seller.not.associated.with.product", new Object[]{product.getName()}, locale));

        if (product.isDeleted())
            throw new ProductNotFoundException(messageSource.getMessage("product.deleted.product", new Object[]{productId}, locale));

        Category category = categoryRepository.findById(product.getCategory().getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(messageSource.getMessage("category.not.found", new Object[]{product.getCategory().getCategoryId()}, locale)));


        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryId(category.getCategoryId());
        categoryDTO.setCategoryName(category.getName());

        return ProductDTO.builder()
                .category(categoryDTO)
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .cancellable(product.isCancellable())
                .returnable(product.isReturnable())
                .active(product.isActive())
                .build();
    }

    public void addProductVariations(ProductVariationDTO dto, MultipartFile primaryImage, List<MultipartFile> secondaryImages) {
        Locale locale = LocaleContextHolder.getLocale();

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(messageSource.getMessage("product.not.found", new Object[]{dto.getProductId()}, locale)));
        if (product.isDeleted()) {
            throw new InvalidProductStateException(messageSource.getMessage("product.deleted", null, locale));
        }
        if (!product.isActive()) {
            throw new InvalidProductStateException(messageSource.getMessage("product.inactive", null, locale));
        }
        Category category = product.getCategory();

        boolean alreadyExists = product.getVariations().stream()
                .anyMatch(v -> Objects.equals(v.getMetadata(), dto.getMetadata()));
        if (alreadyExists)
            throw new DuplicateResourceException(messageSource.getMessage("product.variation.duplicate", null, locale));

        validateMetadata(dto.getMetadata(), category, locale, product);

        ProductVariation variation = new ProductVariation();
        variation.setProduct(product);
        variation.setMetadata(dto.getMetadata());
        variation.setQuantityAvailable(dto.getQuantity());
        variation.setPrice(dto.getPrice());
        variation.setActive(true);

        // Save first to get an ID
        ProductVariation savedVariation = productVariationRepository.save(variation);

        try {
            String primaryImageKey = s3Service.uploadProductVariationImage(product.getId(), savedVariation.getProductVariationId(), primaryImage, true);
            savedVariation.setPrimaryImageName(primaryImageKey);

            if (secondaryImages != null && !secondaryImages.isEmpty()) {
                for (MultipartFile secondaryImage : secondaryImages) {
                    s3Service.uploadProductVariationImage(product.getId(), savedVariation.getProductVariationId(), secondaryImage, false);
                }
            }

            productVariationRepository.save(savedVariation);

        } catch (IOException e) {
            log.error("Error uploading images: {}", e.getMessage());
            productVariationRepository.delete(savedVariation);
            throw new RuntimeException("Failed to upload images: " + e.getMessage());
        }

    }

    private void validateMetadata(Map<String, Object> metadata, Category category, Locale locale, Product product) {
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

    public ProductVariationDTO getProductVariation(UserPrincipal userPrincipal, String variationId) {
        Locale locale = LocaleContextHolder.getLocale();

        ProductVariation productVariation = productVariationRepository.findById(variationId)
                .orElseThrow(() -> new ProductVariationNotFoundException(messageSource.getMessage("product.variation.not.found", new Object[]{variationId}, locale)));

        String email = userPrincipal.getUsername();
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));


        Product product = productVariation.getProduct();

        if (product.getSeller() != seller)
            throw new InvalidSellerException(messageSource.getMessage("seller.not.associated.with.product", new Object[]{product.getName()}, locale));

        if (product.isDeleted())
            throw new ProductNotFoundException(messageSource.getMessage("product.deleted.product", new Object[]{product.getId()}, locale));

        Category category = product.getCategory();



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

    public void deleteProduct(UserPrincipal userPrincipal, String productId) {
        Locale locale = LocaleContextHolder.getLocale();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(messageSource.getMessage("product.not.found", new Object[]{productId}, locale)));

        String email = userPrincipal.getUsername();
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));
        if (product.getSeller() != seller)
            throw new InvalidSellerException(messageSource.getMessage("seller.not.associated.with.product", new Object[]{product.getName()}, locale));

        product.setDeleted(true);
        productRepository.save(product);

    }

    public void updateProductVariation(UserPrincipal userPrincipal, String variationId, ProductVariationUpdateDTO dto, MultipartFile primaryImage, List<MultipartFile> secondaryImages) {
        Locale locale = LocaleContextHolder.getLocale();

        ProductVariation productVariation = productVariationRepository.findById(variationId)
                .orElseThrow(() -> new ProductVariationNotFoundException(messageSource.getMessage("product.variation.not.found", new Object[]{variationId}, locale)));

        String email = userPrincipal.getUsername();
        Seller seller = sellerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));

        Product product = productVariation.getProduct();

        if (product.getSeller() != seller)
            throw new InvalidSellerException(messageSource.getMessage("seller.not.associated.with.product", new Object[]{product.getName()}, locale));

        if (product.isDeleted())
            throw new ProductNotFoundException(messageSource.getMessage("product.deleted", null, locale));

        if (!product.isActive())
            throw new InvalidProductStateException(messageSource.getMessage("product.inactive", null, locale));

        if (dto != null) {
            if (dto.getQuantity() != null)
                productVariation.setQuantityAvailable(dto.getQuantity());

            if (dto.getPrice() != null)
                productVariation.setPrice(dto.getPrice());

            if (dto.getMetadata() != null && !dto.getMetadata().isEmpty()) {
                validateMetadata(dto.getMetadata(), product.getCategory(), locale, product);
                productVariation.setMetadata(dto.getMetadata());
            }
        }

        try {
            if (primaryImage != null && !primaryImage.isEmpty()) {
                // Delete old primary image if it exists
                if (productVariation.getPrimaryImageName() != null) {
                    s3Service.deleteObject(productVariation.getPrimaryImageName());
                }

                String primaryImageKey = s3Service.uploadProductVariationImage(
                        product.getId(), variationId, primaryImage, true);
                productVariation.setPrimaryImageName(primaryImageKey);
            }

            if (secondaryImages != null && !secondaryImages.isEmpty()) {
                // For secondary images, we'd need to track and manage them
                // This is a simplified approach - you may want to enhance it to track each secondary image
                for (MultipartFile secondaryImage : secondaryImages) {
                    if (secondaryImage != null && !secondaryImage.isEmpty()) {
                        s3Service.uploadProductVariationImage(product.getId(), variationId, secondaryImage, false);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error updating images: {}", e.getMessage());
            throw new RuntimeException("Failed to update images: " + e.getMessage());
        }

        productVariationRepository.save(productVariation);
    }
}
