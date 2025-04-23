package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.mapper.Mapper;
import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataField;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataFieldValueId;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataFieldValues;
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
    private final CategoryMetadataFieldValuesRepository categoryMetadataFieldValuesRepository;
    private final MessageSource messageSource;
    private final UserRepository userRepository;


    public List<CustomerResponseDTO> getCustomers(int pageSize, int pageOffset, String sort) {
        Pageable pageable = PageRequest.of(pageOffset, pageSize, Sort.by(sort));
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        return customerPage.stream().map(Mapper::fromCustomer).toList();
    }

    public List<SellerResponseDTO> getSellers(int pageSize, int pageOffset, String sort) {
        Pageable pageable = PageRequest.of(pageOffset, pageSize, Sort.by(sort));
        Page<Seller> sellerPage = sellerRepository.findAll(pageable);
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


    public List<MetadataFieldDTO> getALlMetadataFields(int max, int offset, String sort, String order, String query) {
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
        String newCategoryName = categoryDTO.getCategoryName().trim();
        Locale locale = LocaleContextHolder.getLocale();
        String parentId = categoryDTO.getParentId();

        if (isDuplicateInRoot(newCategoryName))
            throw new DuplicateRootCategoryException(messageSource.getMessage("category.root.duplicate", new Object[]{newCategoryName}, locale));

        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findByCategoryId(parentId);
            if (parent == null)
                throw new ParentCategoryNotFoundException(messageSource.getMessage("category.parent.not.found", new Object[]{parentId}, locale));
            if (isDuplicateInSiblings(parent, newCategoryName))
                throw new DuplicateSiblingCategoryException(messageSource.getMessage("category.sibling.duplicate", new Object[]{newCategoryName, parent.getName()}, locale));
            if (isParentCategoryAssociatedWithProduct(parent))
                throw new ParentCategoryHasProductsException(messageSource.getMessage("category.parent.has.products", new Object[]{parentId}, locale));
            if (isDuplicateInHierarchy(parent, newCategoryName))
                throw new DuplicateInParentHierarchyException(messageSource.getMessage("category.hierarchy.duplicate", new Object[]{newCategoryName}, locale));
        }
        saveCategory(newCategoryName, parent);
    }

    private void saveCategory(String name, Category parent) {
        Category category = Category.builder()
                .name(name)
                .parentCategory(parent)
                .build();
        categoryRepository.save(category);
    }

    private boolean isDuplicateInRoot(String categoryName) {
        return categoryRepository.findByParentCategory_CategoryId(null)
                .stream()
                .anyMatch(cat -> cat.getName().equalsIgnoreCase(categoryName));
    }

    private boolean isDuplicateInSiblings(Category parent, String categoryName) {
        return categoryRepository.findByParentCategory_CategoryId(parent.getCategoryId())
                .stream()
                .anyMatch(cat -> cat.getName().equalsIgnoreCase(categoryName));
    }

    private boolean isDuplicateInHierarchy(Category parent, String categoryName) {
        while (parent != null) {
            if (parent.getName().equalsIgnoreCase(categoryName)) {
                return true;
            }
            parent = parent.getParentCategory();
        }
        return false;
    }

    private boolean isParentCategoryAssociatedWithProduct(Category parent) {
        return productRepository.existsByCategory(parent);
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------

    public CategoryResponseDTO getCategory(String categoryId) {
        Locale locale = LocaleContextHolder.getLocale();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(messageSource.getMessage("category.not.found", new Object[]{categoryId}, locale)));
        return buildCategoryResponseDTO(category);
    }

    private CategoryResponseDTO buildCategoryResponseDTO(Category category) {
        CategoryResponseDTO responseDTO = new CategoryResponseDTO();
        responseDTO.setCategoryId(category.getCategoryId());
        responseDTO.setName(category.getName());

        //parent
        if (category.getParentCategory() != null)
            responseDTO.setParentHierarchy(buildParentHierarchy(category));

        //adding immediate children categories
        if (!category.getSubCategories().isEmpty()) {
            List<CategoryDTO> childrenDTOs = category.getSubCategories().stream()
                    .map(child -> CategoryDTO.builder()
                            .categoryName(child.getName())
                            .categoryId(child.getCategoryId())
                            .parentId(category.getCategoryId())
                            .build())
                    .toList();
            responseDTO.setChildren(childrenDTOs);
        }

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
            CategoryDTO parentDTO = CategoryDTO.builder()
                    .categoryName(current.getName())
                    .parentId(current.getParentCategory() != null ?
                            current.getParentCategory().getCategoryId() : null)
                    .categoryId(current.getCategoryId())
                    .build();
            hierarchy.add(0, parentDTO); // Add at beginning to maintain root->leaf order
            current = current.getParentCategory();
        }
        return hierarchy;
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
                .map(this::buildCategoryResponseDTO)
                .toList();
    }

    public void updateCategory(CategoryUpdateDTO categoryUpdateDTO) {
        Locale locale = LocaleContextHolder.getLocale();

        Category category = categoryRepository.findById(categoryUpdateDTO.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(messageSource.getMessage("category.not.found", new Object[]{categoryUpdateDTO.getCategoryId()}, locale)));

        String updatedName = categoryUpdateDTO.getCategoryName();
        Category parentCategory = category.getParentCategory();


        if (isDuplicateInRoot(updatedName))
            throw new DuplicateRootCategoryException(messageSource.getMessage("category.root.duplicate", new Object[]{updatedName}, locale));
        if (isDuplicateInSiblings(parentCategory, updatedName))
            throw new DuplicateSiblingCategoryException(messageSource.getMessage("category.sibling.duplicate", new Object[]{updatedName,parentCategory.getName()}, locale));
        if (isDuplicateInHierarchy(parentCategory, updatedName))
            throw new DuplicateInParentHierarchyException(messageSource.getMessage("category.hierarchy.duplicate", new Object[]{updatedName}, locale));
        if (isDuplicateInDescendants(category, updatedName)) {
            throw new DuplicateInParentHierarchyException(messageSource.getMessage("category.descendants.duplicate", new Object[]{updatedName}, locale));
        }

        category.setName(updatedName);
        categoryRepository.save(category);
    }

    private boolean isDuplicateInDescendants(Category parent, String updatedName) {
        if (parent.getSubCategories().isEmpty())
            return false;

        for (Category child : parent.getSubCategories()) {
            if (child.getName().equals(updatedName))
                return true;

            isDuplicateInDescendants(child, updatedName);
        }
        return false;
    }

    public void addCategoryMetadataFieldForCategory(String categoryId, List<CategoryMetadataFieldValueDTO> categoryMetadataFieldValueDTO) {

        Locale locale = LocaleContextHolder.getLocale();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(messageSource.getMessage("category.not.found", new Object[]{categoryId}, locale)));

        for (CategoryMetadataFieldValueDTO fieldValueDTO : categoryMetadataFieldValueDTO) {
            CategoryMetadataField categoryMetadataField = categoryMetadataFieldRepository.findById(fieldValueDTO.getFieldId())
                    .orElseThrow(() -> new MetadataFieldNotFoundException(messageSource.getMessage("metadata.field.not.found", new Object[]{fieldValueDTO.getFieldId()},locale)));

            Set<String> uniqueValues = getStrings(fieldValueDTO, categoryMetadataField, locale);


            CategoryMetadataFieldValueId id = new CategoryMetadataFieldValueId(categoryId,fieldValueDTO.getFieldId());


            CategoryMetadataFieldValues fieldValue = new CategoryMetadataFieldValues();
            fieldValue.setId(id);
            fieldValue.setCategory(category);
            fieldValue.setCategoryMetadataField(categoryMetadataField);
            fieldValue.setValues(String.join(",", uniqueValues));

            categoryMetadataFieldValuesRepository.save(fieldValue);
        }

    }

    private Set<String> getStrings(CategoryMetadataFieldValueDTO fieldValueDTO, CategoryMetadataField categoryMetadataField, Locale locale) {
        String values = fieldValueDTO.getValues();

        if(values ==null || values.isBlank())
            throw new InvalidMetadataFieldValueException(messageSource.getMessage("metadata.values.required", new Object[]{categoryMetadataField.getName()}, locale));

        String[]  valuesArray = values.split(",");
        if (valuesArray.length == 0)
            throw new InvalidMetadataFieldValueException(messageSource.getMessage("metadata.values.required", new Object[]{categoryMetadataField.getName()}, locale));

        Set<String> uniqueValues = new HashSet<>();


        for(String value : valuesArray){
            String trimmedValue = value.trim();
            if(trimmedValue.isEmpty())  //for cases : 1,2,3,,4
                continue;
            if(!uniqueValues.add(trimmedValue))
                throw new DuplicateMetadataAssignmentException(messageSource.getMessage("metadata.values.duplicate",new Object[]{trimmedValue, categoryMetadataField.getName()}, locale));
        }
        return uniqueValues;
    }

    public void updateCategoryMetadataFieldForCategory(String categoryId, List<CategoryMetadataFieldValueDTO> categoryMetadataFieldValueDTO) {
        Locale locale = LocaleContextHolder.getLocale();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(messageSource.getMessage("category.not.found", new Object[]{categoryId}, locale)));

        for(CategoryMetadataFieldValueDTO fieldValueDTO : categoryMetadataFieldValueDTO) {
            CategoryMetadataField categoryMetadataField = categoryMetadataFieldRepository.findById(fieldValueDTO.getFieldId())
                    .orElseThrow(() -> new MetadataFieldNotFoundException(messageSource.getMessage("metadata.field.not.found", new Object[]{fieldValueDTO.getFieldId()},locale)));

            CategoryMetadataFieldValueId id = new CategoryMetadataFieldValueId(categoryId,fieldValueDTO.getFieldId());

            if(!categoryMetadataFieldValuesRepository.existsById(id))
                throw new MetadataFieldNotAssociatedWithCategoryException(messageSource.getMessage("metadata.not.associated.with.category", new Object[]{categoryMetadataField.getName(), category.getName()}, locale));

            CategoryMetadataFieldValues existingFieldValue = categoryMetadataFieldValuesRepository.findById(id).get();

            Set<String> uniqueNewValues = getStrings(fieldValueDTO, categoryMetadataField, locale);

            Set<String> existingValues = new HashSet<>(Arrays.asList(existingFieldValue.getValues().split(",")));

            for (String newValue : uniqueNewValues)
                if (existingValues.contains(newValue))
                    throw new DuplicateMetadataAssignmentException(messageSource.getMessage("metadata.value.duplicate", new Object[]{newValue, categoryMetadataField.getName()}, locale));


            // Combine the values
            existingValues.addAll(uniqueNewValues);

            // Update the values
            existingFieldValue.setValues(String.join(",", existingValues));
            categoryMetadataFieldValuesRepository.save(existingFieldValue);
        }
    }



    public void deactivateProduct(String productId) throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(messageSource.getMessage("product.not.found", new Object[]{productId}, locale)));

        if (!product.isActive()) {
            throw new InvalidProductStateException(messageSource.getMessage("product.already.inactive", new Object[]{productId}, locale));
        }

        product.setActive(false);
        productRepository.save(product);

        String sellerEmail = product.getSeller().getEmail();
        emailService.sendProductDeactivationEmail(sellerEmail, "Product Deactivated", product);
    }

    public void activateProduct(String productId) throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(messageSource.getMessage("product.not.found", new Object[]{productId}, locale)));

        if (product.isActive()) {
            throw new InvalidProductStateException(messageSource.getMessage("product.already.active", new Object[]{productId}, locale));
        }

        product.setActive(true);
        productRepository.save(product);

        String sellerEmail = product.getSeller().getEmail();
        emailService.sendProductActivationEmail(sellerEmail, "Product Activated", product);
    }

    public ProductResponseDTO getProduct(String productId) {
        Locale locale = LocaleContextHolder.getLocale();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(messageSource.getMessage("product.not.found", new Object[]{productId}, locale)));

        CategoryDTO categoryDTO = CategoryDTO.builder()
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getName())
                .build();

        ProductDTO productDTO = ProductDTO.builder()
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .cancellable(product.isCancellable())
                .returnable(product.isReturnable())
                .active(product.isActive())
                .category(categoryDTO)
                .build();

        //map product variations
        List<ProductVariationResponseDTO> variationDTOs = product.getVariations().stream()
                .map(variation -> {
                    String primaryImageUrl = null;

                    //get primary image URL
                    if (variation.getPrimaryImageName() != null && !variation.getPrimaryImageName().isEmpty()) {
                        try {
                            primaryImageUrl = s3Service.getObjectUrl(variation.getPrimaryImageName());
                        } catch (IOException e) {
                            log.error("Error getting primary image URL for variation {}: {}",
                                    variation.getProductVariationId(), e.getMessage());
                        }
                    }

                    //get secondary image URLs
                    List<String> secondaryImageUrls = s3Service.getSecondaryImageUrls(
                            product.getId(), variation.getProductVariationId());

                    return ProductVariationResponseDTO.builder()
                            .metadata(variation.getMetadata())
                            .quantity(variation.getQuantityAvailable())
                            .price(variation.getPrice())
                            .primaryImageUrl(primaryImageUrl)
                            .secondaryImageUrl(secondaryImageUrls)
                            .build();
                })
                .collect(Collectors.toList());

        return ProductResponseDTO.builder()
                .product(productDTO)
                .productVariation(variationDTOs)
                .build();
    }


    public List<ProductDTO> searchProducts(Map<String, String> filters, int page, int size, String sort, String direction, UserPrincipal userPrincipal) {
        Locale locale = LocaleContextHolder.getLocale();

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, sortDirection, sort);

        Specification<Product> spec = ProductSpecification.withFilters(filters);
        Page<Product> productsPage = productRepository.findAll(spec, pageable);

        return productsPage.getContent().stream().map(product -> {
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
        }).toList();
    }
}



