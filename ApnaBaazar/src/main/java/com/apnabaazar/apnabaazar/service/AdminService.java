package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.mapper.Mapper;
import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataField;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataFieldValueId;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataFieldValues;
import com.apnabaazar.apnabaazar.model.dto.AdminProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.UserDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.*;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.model.products.Product;
import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.*;
import com.apnabaazar.apnabaazar.specification.ProductSpecification;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class AdminService {

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final CategoryMetadataFieldRepository categoryMetadataFieldRepository;
    private final CategoryRepository categoryRepository;
    private final EmailService emailService;
    private final S3Service s3Service;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final CategoryMetadataFieldValuesRepository categoryMetadataFieldValuesRepository;
    private final MessageSource messageSource;
    private final UserRepository userRepository;
    private final UserService userService;


    public List<CustomerResponseDTO> getCustomers(int pageSize, int pageOffset, String sort, String email) {
        Pageable pageable = PageRequest.of(pageOffset, pageSize, Sort.by(sort));
        Page<Customer> customerPage;
        if (email != null && !email.isBlank())
            customerPage = customerRepository.findByEmailContainingIgnoreCase(email, pageable);
        else
            customerPage = customerRepository.findAll(pageable);
        return customerPage.stream().map(Mapper::fromCustomer).toList();
    }

    public List<SellerResponseDTO> getSellers(int pageSize, int pageOffset, String sort, String email) {
        Pageable pageable = PageRequest.of(pageOffset, pageSize, Sort.by(sort));
        Page<Seller> sellerPage;
        if (email != null && !email.isBlank())
            sellerPage = sellerRepository.findByEmailContainingIgnoreCase(email, pageable);
        else
            sellerPage = sellerRepository.findAll(pageable);
        return sellerPage.stream().map(Mapper::fromSeller).toList();
    }

    public ResponseEntity<GenericResponseDTO> activateCustomer(String id) throws MessagingException {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Customer not found with this ID."));

        if (customer.isActive()) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Customer is already active."));
        }
        customer.setActive(true);
        customerRepository.save(customer);
        emailService.sendVerificationSuccessEmail(customer.getEmail(), "Account Activated");

        return ResponseEntity.ok(new GenericResponseDTO(true, "Customer account activated successfully."));
    }

    public ResponseEntity<GenericResponseDTO> activateSeller(String id) throws MessagingException {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Seller not found with this ID."));

        if (seller.isActive()) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Seller is already active."));
        }
        seller.setActive(true);
        sellerRepository.save(seller);
        emailService.sendVerificationSuccessEmail(seller.getEmail(), "Account Activated");

        return ResponseEntity.ok(new GenericResponseDTO(true, "Seller account activated successfully."));
    }

    public ResponseEntity<GenericResponseDTO> deActivateCustomer(String id) throws MessagingException {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Customer not found with this ID."));
        if (!customer.isActive()) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Customer is already deactivate."));
        }
        customer.setActive(false);
        customerRepository.save(customer);
        emailService.sendAccountDeactivationEmail(customer.getEmail(), "Account Deactivated");
        return ResponseEntity.ok(new GenericResponseDTO(true, "Account deactivated successfully."));
    }

    public ResponseEntity<GenericResponseDTO> deActivateSeller(String id) throws MessagingException {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Seller not found with this ID."));
        if (!seller.isActive()) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Seller is already deactivate."));
        }
        seller.setActive(false);
        sellerRepository.save(seller);
        emailService.sendAccountDeactivationEmail(seller.getEmail(), "Account Deactivated");
        return ResponseEntity.ok(new GenericResponseDTO(true, "Account deactivated successfully."));
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------


    public void addMetadataField(MetadataFieldDTO metadataFieldDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        if (categoryMetadataFieldRepository.existsByName(metadataFieldDTO.getFieldName()))
            throw new DuplicateResourceException(messageSource.getMessage("metadata.field.already.exists", null, locale));

        CategoryMetadataField categoryMetadataField = new CategoryMetadataField();
        categoryMetadataField.setName(metadataFieldDTO.getFieldName());
        categoryMetadataFieldRepository.save(categoryMetadataField);
    }


    public List<MetadataFieldDTO> getAllMetadataFields(int max, int offset, String sort, String order, String query) {
        Sort.Direction direction = Sort.Direction.fromOptionalString(order).orElse(Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(offset, max, Sort.by(direction, sort));

        Page<CategoryMetadataField> metadataFieldPage;
        if (query != null && !query.isBlank())
            metadataFieldPage = categoryMetadataFieldRepository.findByNameContainingIgnoreCase(query, pageable);
        else
            metadataFieldPage = categoryMetadataFieldRepository.findAll(pageable);
        return metadataFieldPage.stream().map(Mapper::fromMetadataField).toList();
    }


    //-------------------------------------------------------------------------------------------------------------------------------------------

    public void addCategory(CategoryDTO categoryDTO) {
        Category parent = null;
        if (categoryDTO.getParentId() != null) {
            parent = categoryRepository.findByCategoryId(categoryDTO.getParentId());
            if (parent == null)
                throw new ParentCategoryNotFoundException(messageSource.getMessage("category.parent.not.found", new Object[]{categoryDTO.getParentId()}, LocaleContextHolder.getLocale()));
        }
        categoryService.validateNewCategoryName(categoryDTO.getCategoryName().trim(), parent);
        categoryService.saveCategory(categoryDTO.getCategoryName().trim(), parent);
    }


    //-------------------------------------------------------------------------------------------------------------------------------------------

    public CategoryResponseDTO getCategory(String categoryId) {
        Category category = categoryService.getCategoryById(categoryId);
        return categoryService.buildCategoryResponseDTO(category);
    }


    public List<CategoryResponseDTO> getAllCategories(int max, int offset, String sort, String order, String query) {
        Sort.Direction direction = Sort.Direction.fromOptionalString(order).orElse(Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(offset, max, Sort.by(direction, sort));

        Page<Category> categoryPage;
        if (query != null && !query.isBlank()) {
            categoryPage = categoryRepository.findByNameContainingIgnoreCase(query, pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }

        return categoryPage.stream()
                .map(categoryService::buildCategoryResponseDTO)
                .toList();
    }

    public void updateCategory(CategoryUpdateDTO categoryUpdateDTO) {
        Category category = categoryService.getCategoryById(categoryUpdateDTO.getCategoryId());
        String updatedName = categoryUpdateDTO.getCategoryName().trim();
        Category parentCategory = category.getParentCategory();
        categoryService.validateUpdatedCategoryName(updatedName,category, parentCategory);
        category.setName(updatedName);
        categoryRepository.save(category);
    }



    public void addCategoryMetadataFieldValuesForCategory(String categoryId, List<CategoryMetadataFieldValueDTO> fieldValueDTOList) {
        Locale locale = LocaleContextHolder.getLocale();
        Category category = categoryService.getCategoryById(categoryId);

        for (CategoryMetadataFieldValueDTO fieldValueDTO : fieldValueDTOList) {
            CategoryMetadataFieldValueId id = new CategoryMetadataFieldValueId(categoryId, fieldValueDTO.getFieldId());
            if(categoryMetadataFieldValuesRepository.existsById(id))
                throw new DuplicateResourceException(messageSource.getMessage("metadata.field.already.exists", null, locale));

            CategoryMetadataField field = categoryService.fetchMetadataField(fieldValueDTO.getFieldId(), locale);
            Set<String> uniqueValues = categoryService.extractUniqueMetadataValues(fieldValueDTO.getValues(), field.getName(), locale);

            CategoryMetadataFieldValues fieldValue = new CategoryMetadataFieldValues();
            fieldValue.setId(new CategoryMetadataFieldValueId(categoryId, fieldValueDTO.getFieldId()));
            fieldValue.setCategory(category);
            fieldValue.setCategoryMetadataField(field);
            fieldValue.setValues(String.join(",", uniqueValues));

            categoryMetadataFieldValuesRepository.save(fieldValue);
        }
    }

    public void updateCategoryMetadataFieldValuesForCategory(String categoryId, List<CategoryMetadataFieldValueDTO> fieldValueDTOList) {
        Locale locale = LocaleContextHolder.getLocale();
        Category category = categoryService.getCategoryById(categoryId);

        for (CategoryMetadataFieldValueDTO fieldValueDTO : fieldValueDTOList) {
            CategoryMetadataField field = categoryService.fetchMetadataField(fieldValueDTO.getFieldId(), locale);
            CategoryMetadataFieldValueId id = new CategoryMetadataFieldValueId(categoryId, fieldValueDTO.getFieldId());

            CategoryMetadataFieldValues existingValue = categoryMetadataFieldValuesRepository.findById(id)
                    .orElseThrow(() -> new MetadataFieldNotAssociatedWithCategoryException(
                            messageSource.getMessage("metadata.not.associated.with.category", new Object[]{field.getName(), category.getName()}, locale)
                    ));

            Set<String> newValues = categoryService.extractUniqueMetadataValues(fieldValueDTO.getValues(), field.getName(), locale);
            Set<String> existingValues = new HashSet<>(Arrays.asList(existingValue.getValues().split(",")));

            for (String newValue : newValues) {
                if (existingValues.contains(newValue)) {
                    throw new DuplicateMetadataAssignmentException(
                            messageSource.getMessage("metadata.value.duplicate", new Object[]{newValue, field.getName()}, locale));
                }
            }

            existingValues.addAll(newValues);
            existingValue.setValues(String.join(",", existingValues));
            categoryMetadataFieldValuesRepository.save(existingValue);
        }
    }


    public void deactivateProduct(String productId) throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();
        Product product = productService.getProductById(productId);
        if (!product.isActive()) {
            throw new InvalidProductStateException(messageSource.getMessage("product.already.inactive", new Object[]{productId}, locale));
        }
        product.setActive(false);
//        product.getVariations().forEach(variation -> variation.setActive(false));
        productRepository.save(product);

        String sellerEmail = product.getSeller().getEmail();
        emailService.sendProductDeactivationEmail(sellerEmail, "Product Deactivated", product);
    }

    public void activateProduct(String productId) throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();
        Product product = productService.getProductById(productId);
        if (product.isActive()) {
            throw new InvalidProductStateException(messageSource.getMessage("product.already.active", new Object[]{productId}, locale));
        }
        product.setActive(true);
        productRepository.save(product);
        String sellerEmail = product.getSeller().getEmail();
        emailService.sendProductActivationEmail(sellerEmail, "Product Activated", product);
    }


    public ProductResponseDTO getProduct(String productId) {
        Product product = productService.getProductById(productId);
        User seller = product.getSeller();
        ProductDTO productDTO = productService.buildProductDTO(product);

        //map product variations
        List<ProductVariationResponseDTO> variationDTOs = product.getVariations().stream()
                .map(variation -> {
                    String primaryImageUrl = productService.getPrimaryImageUrl(variation.getPrimaryImageName());
                    return ProductVariationResponseDTO.builder()
                            .metadata(variation.getMetadata())
                            .quantity(variation.getQuantityAvailable())
                            .price(variation.getPrice())
                            .primaryImageUrl(primaryImageUrl)
                            .build();
                })
                .toList();

        return ProductResponseDTO.builder()
                .sellerId(seller.getId())
                .product(productDTO)
                .productVariation(variationDTOs)
                .build();
    }

    public List<ProductResponseDTO> searchProducts(Map<String, String> filters, int page, int size, String sort, String direction, UserPrincipal userPrincipal) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, sortDirection, sort);
        Specification<Product> spec = ProductSpecification.withFilters(filters,null);
        return productService.buildProductResponseDTOs(pageable, spec, productRepository);
    }

    public UserDTO getProfile(UserPrincipal userPrincipal) {
        User admin = userRepository.findByEmail(userPrincipal.getUsername()).orElseThrow(() -> new UserNotFoundException(userPrincipal.getUsername()));

        return UserDTO.builder()
                .firstName(admin.getFirstName())
                .middleName(admin.getMiddleName())
                .lastName(admin.getLastName())
                .email(admin.getEmail())
                .build();
    }

    public void updateProfile(UserPrincipal userPrincipal, AdminProfileUpdateDTO adminDTO) {
        User admin = userRepository.findByEmail(userPrincipal.getUsername()).orElseThrow(() -> new UserNotFoundException("Admin not found"));
        admin.setFirstName(userService.getUpdatedValue(adminDTO.getFirstName(), admin.getFirstName()));
        admin.setMiddleName(userService.getUpdatedValue(adminDTO.getMiddleName(), admin.getMiddleName()));
        admin.setLastName(userService.getUpdatedValue(adminDTO.getLastName(), admin.getLastName()));
        userRepository.save(admin);
    }

    public void updateAdminPassword(UserPrincipal userPrincipal, @Valid UpdatePasswordDTO updatePasswordDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        User admin = userRepository.findByEmail(userPrincipal.getUsername()).orElseThrow(() -> new UserNotFoundException("Admin not found"));
        userService.updatePassword(admin, updatePasswordDTO);
    }

    public void unlockUser(String userId) throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(messageSource.getMessage("seller.not.found", new Object[]{userId}, LocaleContextHolder.getLocale())));
        if (user.isDeleted())
            throw new UserNotFoundException( messageSource.getMessage("user.unlock.deleted", null, locale));
        if (!user.isLocked())
            throw new AccountLockedException(messageSource.getMessage("user.unlock.not.locked", null, locale));
        user.setLocked(false);
        user.setInvalidAttemptCount(0);
        userRepository.save(user);
        emailService.sendAccountUnlockedEmail(user.getEmail(),"Account Unlocked");
    }
}



