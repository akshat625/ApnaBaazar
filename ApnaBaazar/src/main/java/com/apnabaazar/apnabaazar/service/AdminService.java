package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.mapper.Mapper;
import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataField;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.MetadataFieldDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.*;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
@Transactional
public class AdminService {

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private  final ProductRepository productRepository;
    private final CategoryMetadataFieldRepository categoryMetadataFieldRepository;
    private final CategoryRepository categoryRepository;
    private final EmailService emailService;
    private final MessageSource messageSource;


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
                throw new DuplicateSiblingCategoryException(messageSource.getMessage("category.sibling.duplicate", new Object[]{newCategoryName, parentId}, locale));
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
}



