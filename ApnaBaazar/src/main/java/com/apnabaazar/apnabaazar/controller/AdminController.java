package com.apnabaazar.apnabaazar.controller;
import com.apnabaazar.apnabaazar.model.dto.category_dto.*;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.service.AdminService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final MessageSource messageSource;
    private Locale locale;

    @ModelAttribute
    public void initLocale() {
        this.locale = LocaleContextHolder.getLocale();
    }

    @GetMapping("/test")
    public String index(){
        return "Admin Page";
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponseDTO>> getCustomers(
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "0") int pageOffset,
            @RequestParam(defaultValue = "id") String sort
    ) {
        return ResponseEntity.ok(adminService.getCustomers(pageSize, pageOffset, sort));
    }

    @GetMapping("/sellers")
    public ResponseEntity<List<SellerResponseDTO>> getAllSellers(
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "0") int pageOffset,
            @RequestParam(defaultValue = "id") String sort
    ) {
        return ResponseEntity.ok(adminService.getSellers(pageSize, pageOffset, sort));
    }

    @PutMapping("/activate/customer")
    public ResponseEntity<GenericResponseDTO> activateCustomer(@RequestParam String id) throws MessagingException {
        return adminService.activateCustomer(id);
    }

    @PutMapping("/activate/seller")
    public ResponseEntity<GenericResponseDTO> activateSeller(@RequestParam String id) throws MessagingException {
        return adminService.activateSeller(id);
    }

    @PutMapping("/de-activate/customer")
    public ResponseEntity<GenericResponseDTO> deActivateCustomer(@RequestParam String id) throws MessagingException {
        return adminService.deActivateCustomer(id);
    }

    @PutMapping("/de-activate/seller")
    public ResponseEntity<GenericResponseDTO> deActivateSeller(@RequestParam String id) throws MessagingException {
        return adminService.deActivateSeller(id);
    }

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    @PostMapping("/metadata-fields")
    public ResponseEntity<GenericResponseDTO> addMetadataField(@Valid @RequestBody MetadataFieldDTO metadataFieldDTO) {
        adminService.addMetadataField(metadataFieldDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true,messageSource.getMessage("metadata.field.added.success", null, locale)));
    }

    @GetMapping("/metadata-fields")
    public ResponseEntity<List<MetadataFieldDTO>> getAllMetadataFields(
            @RequestParam(required = false, defaultValue = "10") int max,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "asc") String order,
            @RequestParam(required = false, defaultValue = "") String query
    ) {
        return ResponseEntity.ok(adminService.getALlMetadataFields(max, offset, sort, order, query));
    }

    @PostMapping("/category")
    public ResponseEntity<GenericResponseDTO> addCategory(@RequestBody CategoryDTO categoryDTO) {
        adminService.addCategory(categoryDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true,messageSource.getMessage("category.added.success", null, locale)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> getCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(adminService.getCategory(categoryId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories(
            @RequestParam(required = false, defaultValue = "10") int max,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "asc") String order,
            @RequestParam(required = false, defaultValue = "") String query
    ) {
        return ResponseEntity.ok(adminService.getAllCategories(max, offset, sort, order, query));
    }

    @PutMapping("/category")
    public ResponseEntity<GenericResponseDTO> updateCategory(@Valid @RequestBody CategoryUpdateDTO  categoryUpdateDTO) {
            adminService.updateCategory(categoryUpdateDTO);
            return ResponseEntity.ok(new GenericResponseDTO(true,messageSource.getMessage("category.update.success", null, locale)));
    }

    @PostMapping("/category/{categoryId}")
    public ResponseEntity<GenericResponseDTO> addCategoryMetadataFieldForCategory(@PathVariable String categoryId, @Valid @RequestBody List<CategoryMetadataFieldValueDTO> categoryMetadataFieldValueDTO) {
        adminService.addCategoryMetadataFieldForCategory(categoryId, categoryMetadataFieldValueDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true,messageSource.getMessage("category.metadata.added.success", null, locale)));
    }
    @PutMapping("/category/{categoryId}")
    public ResponseEntity<GenericResponseDTO> updateCategoryMetadataFieldForCategory(@PathVariable String categoryId, @Valid @RequestBody List<CategoryMetadataFieldValueDTO> categoryMetadataFieldValueDTO) {
        adminService.updateCategoryMetadataFieldForCategory(categoryId, categoryMetadataFieldValueDTO);
        return ResponseEntity.ok(new GenericResponseDTO(true,messageSource.getMessage("category.metadata.updated.success", null, locale)));
    }



    @PutMapping("/product/activate/{productId}")
    public ResponseEntity<GenericResponseDTO> activateProduct(@PathVariable String productId) throws MessagingException {
        adminService.activateProduct(productId);
        return ResponseEntity.ok(new GenericResponseDTO(true, messageSource.getMessage("product.activated.success", null, locale)));
    }


}
