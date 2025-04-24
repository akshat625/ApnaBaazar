package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.*;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.ProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.products.Product;
import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import com.apnabaazar.apnabaazar.model.users.*;
import com.apnabaazar.apnabaazar.repository.*;
import com.apnabaazar.apnabaazar.specification.ProductSpecification;
import com.apnabaazar.apnabaazar.specification.ProductVariationSpecification;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

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
    private final CategoryService categoryService;
    private final ProductVariationService productVariationService;
    private final UserService userService;
    private final ProductService productService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MessageSource messageSource;
    private final EmailService emailService;
    private final RoleRepository roleRepository;


    @Value("${aws.s3.default-seller-image}")
    private String defaultSellerImage;

    private Seller getSellerByEmail(String email) {
        return (Seller) userService.getUserByEmail(email);
    }

    private String getUpdatedValue(String newValue, String oldValue) {
        return (newValue != null && !newValue.isBlank()) ? newValue : oldValue;
    }


        public ResponseEntity<SellerProfileDTO> getSellerProfile(UserPrincipal userPrincipal) {
        String email = userPrincipal.getUsername();
        log.info("Fetching profile for seller: {}", email);
        Seller seller = getSellerByEmail(email);
        try {
            String imageUrl = s3Service.getProfileImageUrl(email, defaultSellerImage);
            log.info("Seller profile image URL resolved: {}", imageUrl);
            return ResponseEntity.ok(SellerMapper.toSellerProfileDTO(seller, imageUrl));
        } catch (Exception e) {
            log.error("Error retrieving seller profile for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
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
        if (addressUpdateDTO != null)
            userService.updateAddressFields(address, addressUpdateDTO);
        addressRepository.save(address);
        log.info("Address [ID: {}] updated successfully for seller: {}", addressId, email);
    }


    public void updateSellerPassword(UserPrincipal userPrincipal, UpdatePasswordDTO updatePasswordDTO) {
        String email = userPrincipal.getUsername();
        log.info("Updating seller password for seller: {}", email);
        Seller seller = getSellerByEmail(email);
        userService.updatePassword(seller, updatePasswordDTO);
    }

    public List<CategoryResponseDTO> getAllCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        List<Category> leafCategories = allCategories.stream()
                .filter(category -> category.getSubCategories() == null || category.getSubCategories().isEmpty())
                .toList();

        // Map leaf categories to response DTOs with full metadata and hierarchy
        return leafCategories.stream()
                .map(this::buildCategoryResponseDTO)
                .toList();
    }

    private CategoryResponseDTO buildCategoryResponseDTO(Category category) {
        CategoryResponseDTO responseDTO = new CategoryResponseDTO();
        responseDTO.setCategoryId(category.getCategoryId());
        responseDTO.setName(category.getName());
        if (category.getParentCategory() != null)
            responseDTO.setParentHierarchy(categoryService.buildParentHierarchy(category));
        // Collect metadata fields from this category and all parent categories
        responseDTO.setMetadataFields(categoryService.getAllMetadataFieldsFromHierarchy(category));
        return responseDTO;
    }


    public void addProduct(UserPrincipal userPrincipal, @Valid ProductDTO productDTO) throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        Seller seller = getSellerByEmail(email);
        Category category = categoryService.getCategoryById(productDTO.getCategoryId());
        categoryService.validateLeafCategory(category);
        productService. checkForDuplicateProduct(productDTO, seller, category, locale);
        Product product = productService.buildProductFromDTO(productDTO, seller, category);
        productRepository.save(product);

        Optional<Role> role = roleRepository.findByAuthority("ROLE_ADMIN");
        Set<Role> roles = new HashSet<>();
        roles.add(role.get());
        User admin = userRepository.findByRoles(roles);
        emailService.sendProductAddedMail(admin.getEmail(), "Product Added");

    }

    public ProductDTO getProduct(UserPrincipal userPrincipal, String productId) {
        String email = userPrincipal.getUsername();
        Seller seller = getSellerByEmail(email);
        Product product = productService.getProductById(productId);
        productService.validateSellerOwnership(product, seller);
        productService.validateDeleteState(product);
        return productService.buildProductDTO(product);
    }

    public void addProductVariations(ProductVariationDTO dto, MultipartFile primaryImage, List<MultipartFile> secondaryImages) {
        Locale locale = LocaleContextHolder.getLocale();
        Product product = productService.getProductById(dto.getProductId());
        productService.validateDeleteAndActiveState(product);
        Category category = product.getCategory();

        boolean alreadyExists = product.getVariations().stream().anyMatch(v -> Objects.equals(v.getMetadata(), dto.getMetadata()));
        if (alreadyExists)
            throw new DuplicateResourceException(messageSource.getMessage("product.variation.duplicate", null, locale));

        productService.validateMetadata(dto.getMetadata(), category, locale, product);
        ProductVariation variation = new ProductVariation();
        variation.setProduct(product);
        variation.setMetadata(dto.getMetadata());
        variation.setQuantityAvailable(dto.getQuantity());
        variation.setPrice(dto.getPrice());
        variation.setActive(true);

        // Save first to get an ID
        ProductVariation savedVariation = productVariationRepository.save(variation);
        try {
            productVariationService.updatePrimaryImage(product, savedVariation, primaryImage);
            productVariationService.uploadSecondaryImages(product.getId(), savedVariation.getProductVariationId(), secondaryImages);
            productVariationRepository.save(savedVariation);
        } catch (IOException e) {
            log.error("Error uploading images: {}", e.getMessage());
            productVariationRepository.delete(savedVariation);
            throw new RuntimeException("Failed to upload images: " + e.getMessage());
        }
    }


    public ProductVariationDTO getProductVariation(UserPrincipal userPrincipal, String variationId) {
        ProductVariation productVariation = productVariationService.getProductVariationById(variationId);
        String email = userPrincipal.getUsername();
        Seller seller = getSellerByEmail(email);
        Product product = productVariation.getProduct();
        productService.validateSellerOwnership(product, seller);
        productService.validateDeleteState(product);
        return productVariationService.mapToProductVariationDTO(productVariation, product);
    }



    public void deleteProduct(UserPrincipal userPrincipal, String productId) {
        Product product = productService.getProductById(productId);
        String email = userPrincipal.getUsername();
        Seller seller = getSellerByEmail(email);
        productService.validateSellerOwnership(product, seller);
        product.setDeleted(true);
        productRepository.save(product);

    }

    public void updateProductVariation(UserPrincipal userPrincipal, String variationId, ProductVariationUpdateDTO dto, MultipartFile primaryImage, List<MultipartFile> secondaryImages) {
        Locale locale = LocaleContextHolder.getLocale();
        ProductVariation variation = productVariationService.getProductVariationById(variationId);
        Seller seller = getSellerByEmail(userPrincipal.getUsername());
        Product product = variation.getProduct();
        productVariationService.validateSellerOwnership(seller, product, locale);
        productService.validateDeleteAndActiveState(product);
        productVariationService.updateVariationDetails(dto, variation, product, locale);
        productVariationService.updateImages(product, variation, primaryImage, secondaryImages);
        productVariationRepository.save(variation);
    }


    public void updateProduct(UserPrincipal userPrincipal, ProductUpdateDTO dto, String productId) {
        Locale locale = LocaleContextHolder.getLocale();
        Product product = productService.getProductById(productId);
        String email = userPrincipal.getUsername();
        Seller seller = getSellerByEmail(email);
        productService.validateSellerOwnership(product, seller);
        productService.updateProductNameIfValid(dto, product, productId, seller, locale);

        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (Boolean.TRUE.equals(dto.getCancellable()) != product.isCancellable())
            product.setCancellable(dto.getCancellable());
        if (Boolean.TRUE.equals(dto.getReturnable()) != product.isReturnable())
            product.setReturnable(dto.getReturnable());

        productRepository.save(product);
    }

    public List<ProductDTO> searchProducts(Map<String, String> filters, int page, int size, String sort, String direction, UserPrincipal userPrincipal) {
        String email = userPrincipal.getUsername();
        Seller seller = getSellerByEmail(email);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, sortDirection, sort);
        Specification<Product> spec = ProductSpecification.withFilters(filters, seller.getId());
        return productService.getProductDTOS(pageable, spec, productRepository);
    }


    public List<ProductVariationResponseDTO> searchProductVariations(Map<String, Object> filters, int page, int size, String sort, String direction, UserPrincipal userPrincipal, String productId) {
        String email = userPrincipal.getUsername();
        Seller seller = getSellerByEmail(email);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, sortDirection, sort);
        Specification<ProductVariation> spec = ProductVariationSpecification.withFilters(filters, seller.getId());
        Page<ProductVariation> productVariationPage = productVariationRepository.findAll(spec, pageable);

        return productVariationPage.getContent().stream()
                .filter(variation -> variation.getProduct().getId().equals(productId))
                .map(variation -> productVariationService.mapToProductVariationResponseDTO(variation, productId))
                .toList();
    }
}
