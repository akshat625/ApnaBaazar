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
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.ProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.products.Product;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

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

            return ResponseEntity.ok(SellerMapper.toSellerProfileDTO(seller,imageUrl));
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
                .orElseThrow(()-> new UsernameNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{email}, locale)));
        if (sellerProfileUpdateDTO != null){
            seller.setFirstName(getUpdatedValue(sellerProfileUpdateDTO.getFirstName(), seller.getFirstName()));
            seller.setMiddleName(getUpdatedValue(sellerProfileUpdateDTO.getMiddleName(), seller.getMiddleName()));
            seller.setLastName(getUpdatedValue(sellerProfileUpdateDTO.getLastName(), seller.getLastName()));
            seller.setCompanyContact(getUpdatedValue(sellerProfileUpdateDTO.getContact(), seller.getCompanyContact()));
       }
        sellerRepository.save(seller);
        log.info("Seller profile updated successfully for: {}", email);
    }

    public void updateSellerAddress(UserPrincipal userPrincipal,String addressId, AddressUpdateDTO addressUpdateDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = userPrincipal.getUsername();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException(messageSource.getMessage("address.not.found", new Object[]{addressId}, locale)));
        log.info("Updating address [ID: {}] for seller: {}", addressId, email);

        if (addressUpdateDTO != null){
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
        if(!passwordEncoder.matches(updatePasswordDTO.getOldPassword(),seller.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.old.incorrect", null, locale));
        if(passwordEncoder.matches(updatePasswordDTO.getNewPassword(),seller.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.new.password.incorrect", null, locale));
        if(!updatePasswordDTO.getNewPassword().equals(updatePasswordDTO.getConfirmPassword())) {
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
                                .collect(Collectors.toList())
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

        if(!category.getSubCategories().isEmpty())
            throw new InvalidLeafCategoryException(messageSource.getMessage("category.not.leaf", new Object[]{category.getName()}, locale));

        boolean isDuplicate = productRepository.existsByNameAndBrandAndCategoryAndSeller(productDTO.getName(),productDTO.getBrand(),category,seller);
        if (isDuplicate)
            throw new DuplicateProductException(messageSource.getMessage("product.duplicate.name", new Object[]{productDTO.getName(), productDTO.getBrand()}, locale));

        Product product = Product.builder()
                .name(productDTO.getName())
                .brand(productDTO.getBrand())
                .category(category)
                .seller(seller)
                .cancellable(productDTO.isCancellable())
                .returnable(productDTO.isReturnable())
                .isActive(false)
                .build();

        productRepository.save(product);


        Optional<Role> role = roleRepository.findByAuthority("ROLE_ADMIN");

        Set<Role> roles = new HashSet<>();
        roles.add(role.get());
        User admin = userRepository.findByRoles(roles);
        emailService.sendProductActivationMail(admin.getEmail(),"Product Added");

    }
}
