package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.config.UserPrincipal;
import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.mapper.CustomerMapper;
import com.apnabaazar.apnabaazar.mapper.SellerMapper;
import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryFilterDetailsDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CustomerCategoryResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerProfileDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.product_dto.ProductVariationResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.ProfileUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.products.Product;
import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.*;
import com.apnabaazar.apnabaazar.specification.ProductSpecification;
import com.apnabaazar.apnabaazar.specification.ProductVariationSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final S3Service s3Service;


    @Value("${aws.s3.default-customer-image}")
    private String defaultCustomerImage;

    public ResponseEntity<CustomerProfileDTO> getCustomerProfile(UserPrincipal userPrincipal) {
        String email = userPrincipal.getUsername();
        Customer customer = getCustomerByEmail(email);
        String imageUrl = s3Service.getProfileImageUrl(email, defaultCustomerImage);
        return ResponseEntity.ok(CustomerMapper.toCustomerProfileDTO(customer, imageUrl));
    }

    public void updateCustomerProfile(UserPrincipal userPrincipal, ProfileUpdateDTO profileUpdateDTO) {
        String email = userPrincipal.getUsername();
        Customer customer = getCustomerByEmail(email);

        customer.setFirstName(userService.getUpdatedValue(profileUpdateDTO.getFirstName(), customer.getFirstName()));
        customer.setMiddleName(userService.getUpdatedValue(profileUpdateDTO.getMiddleName(), customer.getMiddleName()));
        customer.setLastName(userService.getUpdatedValue(profileUpdateDTO.getLastName(), customer.getLastName()));
        customer.setContact(userService.getUpdatedValue(profileUpdateDTO.getContact(), customer.getContact()));

        customerRepository.save(customer);
        log.info("Customer profile updated for: {}", email);
    }

    public ResponseEntity<List<AddressDTO>> getCustomerAddresses(UserPrincipal userPrincipal) {
        String email = userPrincipal.getUsername();
        Customer customer = getCustomerByEmail(email);
        log.info("Fetching addresses of Customer : {}", email);
        Set<Address> customerAddresses = customer.getAddresses();
        if (customerAddresses.isEmpty()) {
            log.info("No addresses found for customer: {}", email);
        }
        return ResponseEntity.ok(CustomerMapper.toAllAddressDTO(customerAddresses));
    }



    public void updateCustomerPassword(UserPrincipal userPrincipal, UpdatePasswordDTO updatePasswordDTO) {
        String email = userPrincipal.getUsername();
        Customer customer = getCustomerByEmail(email);
        userService.updatePassword(customer, updatePasswordDTO);
    }

    public void addCustomerAddress(UserPrincipal userPrincipal, AddressDTO addressDTO) {
        Customer customer = getCustomerByEmail(userPrincipal.getUsername());
        Address newAddress = CustomerMapper.toAddress(addressDTO);
        customer.getAddresses().add(newAddress);
        customerRepository.save(customer);
    }

    public void updateCustomerAddress(UserPrincipal userPrincipal, String addressId, AddressUpdateDTO addressUpdateDTO) throws AccessDeniedException {
        Customer customer = getCustomerByEmail(userPrincipal.getUsername());
        userService.updateAddress(customer, addressId, addressUpdateDTO);
    }

    public void deleteCustomerAddress(UserPrincipal userPrincipal, String addressId) throws AccessDeniedException {
        Customer customer = getCustomerByEmail(userPrincipal.getUsername());
        userService.deleteAddress(customer, addressId);
    }
    public ProductResponseDTO getProduct(String productId) {
        return productService.getProduct(productId, false);
    }


    public List<CustomerCategoryResponseDTO> getAllCategories(String categoryId) {
        Locale locale = LocaleContextHolder.getLocale();
        if (categoryId == null || categoryId.isBlank()) {
            List<Category> rootCategories = categoryRepository.findByParentCategory_CategoryId(null);
            return convertToCustomerDTOList(rootCategories);
        }
        Category category = categoryService.getCategoryById(categoryId);
        List<Category> childCategories = category.getSubCategories().stream().toList();
        return convertToCustomerDTOList(childCategories);
    }


    private List<CustomerCategoryResponseDTO> convertToCustomerDTOList(List<Category> categories) {
        return categories.stream()
                .map(cat -> {
                    CustomerCategoryResponseDTO dto = new CustomerCategoryResponseDTO();
                    dto.setId(cat.getCategoryId());
                    dto.setName(cat.getName());
                    return dto;
                })
                .toList();
    }


    public CategoryFilterDetailsDTO getCategoryFilters(String categoryId) {
        Category category = categoryService.getCategoryById(categoryId);
        List<String> categoryIds = categoryService.getAllChildCategoriesIds(category);
        Map<String, String> metadataFilters = categoryService.getCategoryMetadataFilters(category);
        List<Product> products = productRepository.findByCategoryCategoryIdIn(categoryIds);

        List<String> brands = products.stream().map(Product::getBrand).distinct().sorted().toList();

        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0.0;

        for (Product product : products) {
            for (ProductVariation variation : product.getVariations()) {
                if (variation.isActive() && variation.getPrice() != null) {
                    minPrice = Math.min(minPrice, variation.getPrice());
                    maxPrice = Math.max(maxPrice, variation.getPrice());
                }
            }
        }
        //case when no products or variations are found
        if (minPrice == Double.MAX_VALUE) {
            minPrice = 0.0;
        }

        return CategoryFilterDetailsDTO.builder()
                .categoryName(category.getName())
                .metadataFilters(metadataFilters)
                .brands(brands)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
    }


    private Customer getCustomerByEmail(String email) {
        return (Customer) userService.getUserByEmail(email);
    }


    public List<ProductResponseDTO> getAllProducts(String categoryId, Map<String, String> filters, int page, int size, String sort, String direction, UserPrincipal userPrincipal) {
        Category category = categoryService.getCategoryById(categoryId);
        List<String> childCategories = categoryService.getAllChildCategoriesIds(category);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Specification<Product> spec = ProductSpecification.withCustomerFilters(childCategories, filters);
        return productService.buildProductResponseDTOs(pageable, spec, productRepository);
    }

    public List<ProductResponseDTO> getSimilarProducts(String productId, int page, int size, String sort, String direction) {
        Product product = getProductById(productId);

        if (!product.isActive()) {
            throw new ProductNotFoundException("Product is inactive and cannot be used.");
        }

        Category category = product.getCategory();
        Specification<Product> spec = ProductSpecification.withSimilarityFilters(productId, category.getCategoryId());

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        return productService.buildProductResponseDTOs(pageable, spec, productRepository);
    }

    private Product getProductById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    }
}