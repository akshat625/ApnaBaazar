package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.*;
import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataField;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataFieldValues;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryMetadataFieldValueDTO;
import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryResponseDTO;
import com.apnabaazar.apnabaazar.repository.CategoryMetadataFieldRepository;
import com.apnabaazar.apnabaazar.repository.CategoryMetadataFieldValuesRepository;
import com.apnabaazar.apnabaazar.repository.CategoryRepository;
import com.apnabaazar.apnabaazar.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Setter
@RequiredArgsConstructor
@Service
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMetadataFieldRepository categoryMetadataFieldRepository;
    private final CategoryMetadataFieldValuesRepository categoryMetadataFieldValuesRepository;
    private final MessageSource messageSource;

    boolean isDuplicateInRoot(String categoryName) {
        log.debug("Checking for duplicate root category with name: {}", categoryName);
        return categoryRepository.findByParentCategory_CategoryId(null)
                .stream()
                .anyMatch(cat -> cat.getName().equalsIgnoreCase(categoryName));
    }

    boolean isDuplicateInSiblings(Category parent, String categoryName) {
        log.debug("Checking for duplicate category '{}' among siblings of parent category ID: {}", categoryName, parent.getCategoryId());
        return categoryRepository.findByParentCategory_CategoryId(parent.getCategoryId())
                .stream()
                .anyMatch(cat -> cat.getName().equalsIgnoreCase(categoryName));
    }

    boolean isDuplicateInHierarchy(Category parent, String categoryName) {
        log.debug("Checking for '{}' in parent hierarchy", categoryName);
        while (parent != null) {
            if (parent.getName().equalsIgnoreCase(categoryName)) {
                return true;
            }
            parent = parent.getParentCategory();
        }
        return false;
    }

    private boolean isParentCategoryAssociatedWithProduct(Category parent) {
        log.debug("Checking if parent category ID: {} is associated with any product", parent.getCategoryId());
        return productRepository.existsByCategory(parent);
    }

    void saveCategory(String name, Category parent) {
        log.info("Saving new category: '{}' with parent ID: {}", name, parent != null ? parent.getCategoryId() : "null");
        Category category = Category.builder()
                .name(name)
                .parentCategory(parent)
                .build();
        categoryRepository.save(category);
    }

    boolean isDuplicateInDescendants(Category parent, String updatedName) {
        log.debug("Checking if name '{}' exists in descendants of category ID: {}", updatedName, parent.getCategoryId());
        if (parent.getSubCategories().isEmpty())
            return false;

        for (Category child : parent.getSubCategories()) {
            if (child.getName().equals(updatedName))
                return true;

            if (isDuplicateInDescendants(child, updatedName))
                return true;
        }
        return false;
    }

    public void validateNewCategoryName(String categoryName, Category parent) {
        Locale locale = LocaleContextHolder.getLocale();
        log.debug("Validating new category name: '{}' under parent: {}", categoryName, parent != null ? parent.getCategoryId() : "null");

        if (isDuplicateInRoot(categoryName))
            throw new DuplicateRootCategoryException(messageSource.getMessage("category.root.duplicate", new Object[]{categoryName}, locale));
        if (parent != null) {
            if (isDuplicateInSiblings(parent, categoryName))
                throw new DuplicateSiblingCategoryException(messageSource.getMessage("category.sibling.duplicate", new Object[]{categoryName, parent.getName()}, locale));
            if (isParentCategoryAssociatedWithProduct(parent))
                throw new ParentCategoryHasProductsException(messageSource.getMessage("category.parent.has.products", new Object[]{parent.getCategoryId()}, locale));
            if (isDuplicateInHierarchy(parent, categoryName))
                throw new DuplicateInParentHierarchyException(messageSource.getMessage("category.hierarchy.duplicate", new Object[]{categoryName}, locale));
        }
    }

    void validateUpdatedCategoryName(String updatedName, Category currentCategory, Category parentCategory) {
        Locale locale = LocaleContextHolder.getLocale();
        log.debug("Validating updated category name: '{}' for current category ID: {}", updatedName, currentCategory.getCategoryId());

        if (isDuplicateInRoot(updatedName))
            throw new DuplicateRootCategoryException(messageSource.getMessage("category.root.duplicate", new Object[]{updatedName}, locale));
        if (isDuplicateInSiblings(parentCategory, updatedName))
            throw new DuplicateSiblingCategoryException(messageSource.getMessage("category.sibling.duplicate", new Object[]{updatedName, parentCategory.getName()}, locale));
        if (isDuplicateInHierarchy(parentCategory, updatedName))
            throw new DuplicateInParentHierarchyException(messageSource.getMessage("category.hierarchy.duplicate", new Object[]{updatedName}, locale));
        if (isDuplicateInDescendants(currentCategory, updatedName))
            throw new DuplicateInParentHierarchyException(messageSource.getMessage("category.descendants.duplicate", new Object[]{updatedName}, locale));
    }

    public Category getCategoryById(String categoryId) {
        Locale locale = LocaleContextHolder.getLocale();
        log.debug("Fetching category by ID: {}", categoryId);
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", categoryId);
                    return new CategoryNotFoundException(messageSource.getMessage("category.not.found", new Object[]{categoryId}, locale));
                });
    }

    CategoryResponseDTO buildCategoryResponseDTO(Category category) {
        log.debug("Building response DTO for category ID: {}", category.getCategoryId());
        CategoryResponseDTO responseDTO = new CategoryResponseDTO();
        responseDTO.setCategoryId(category.getCategoryId());
        responseDTO.setName(category.getName());

        if (category.getParentCategory() != null)
            responseDTO.setParentHierarchy(buildParentHierarchy(category));

        if (!category.getSubCategories().isEmpty()) {
            List<CategoryDTO> childrenDTOs = category.getSubCategories().stream()
                    .map(child -> CategoryDTO.builder()
                            .categoryName(child.getName())
                            .categoryId(child.getCategoryId())
                            .parentId(category.getCategoryId())
                            .build())
                    .toList();
            responseDTO.setChildren(childrenDTOs);
        } else {
            responseDTO.setChildren(new ArrayList<>());
        }

        responseDTO.setMetadataFields(getAllMetadataFieldsFromHierarchy(category));
        return responseDTO;
    }

    List<CategoryMetadataFieldValueDTO> getAllMetadataFieldsFromHierarchy(Category category) {
        log.debug("Fetching metadata fields from hierarchy for category ID: {}", category.getCategoryId());
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

        return metadataDTOs;
    }

    List<CategoryDTO> buildParentHierarchy(Category category) {
        log.debug("Building parent hierarchy for category ID: {}", category.getCategoryId());
        List<CategoryDTO> hierarchy = new ArrayList<>();
        Category current = category.getParentCategory();

        while (current != null) {
            CategoryDTO parentDTO = CategoryDTO.builder()
                    .categoryName(current.getName())
                    .parentId(current.getParentCategory() != null ?
                            current.getParentCategory().getCategoryId() : null)
                    .categoryId(current.getCategoryId())
                    .build();
            hierarchy.add(0, parentDTO);
            current = current.getParentCategory();
        }
        return hierarchy;
    }

    CategoryMetadataField fetchMetadataField(String fieldId, Locale locale) {
        log.debug("Fetching metadata field by ID: {}", fieldId);
        return categoryMetadataFieldRepository.findById(fieldId)
                .orElseThrow(() -> {
                    log.error("Metadata field not found with ID: {}", fieldId);
                    return new MetadataFieldNotFoundException(
                            messageSource.getMessage("metadata.field.not.found", new Object[]{fieldId}, locale));
                });
    }

    Set<String> extractUniqueMetadataValues(String values, String fieldName, Locale locale) {
        log.debug("Extracting unique metadata values for field: {}", fieldName);
        if (values == null || values.isBlank()) {
            log.error("Metadata values are missing for field: {}", fieldName);
            throw new InvalidMetadataFieldValueException(
                    messageSource.getMessage("metadata.values.required", new Object[]{fieldName}, locale));
        }

        String[] splitValues = values.split(",");
        Set<String> uniqueValues = new HashSet<>();

        for (String value : splitValues) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) continue;
            if (!uniqueValues.add(trimmed)) {
                log.error("Duplicate metadata value '{}' found for field: {}", trimmed, fieldName);
                throw new DuplicateMetadataAssignmentException(messageSource.getMessage("metadata.values.duplicate", new Object[]{trimmed, fieldName}, locale));
            }
        }
        if (uniqueValues.isEmpty()) {
            log.error("No valid metadata values provided for field: {}", fieldName);
            throw new InvalidMetadataFieldValueException(messageSource.getMessage("metadata.values.required", new Object[]{fieldName}, locale));
        }

        return uniqueValues;
    }

    void validateLeafCategory(Category category) {
        log.debug("Validating if category ID: {} is a leaf category", category.getCategoryId());
        if (!category.getSubCategories().isEmpty())
            throw new InvalidLeafCategoryException(messageSource.getMessage("category.not.leaf", new Object[]{category.getName()}, LocaleContextHolder.getLocale()));
    }

    Map<String, String> getCategoryMetadataFilters(Category category) {
        log.debug("Generating metadata filters for category ID: {}", category.getCategoryId());
        Map<String, String> metadataFilters = new HashMap<>();
        Category current = category;

        while (current != null) {
            List<CategoryMetadataFieldValues> fieldValues = categoryMetadataFieldValuesRepository.findByCategory(current);
            for (CategoryMetadataFieldValues fieldValue : fieldValues) {
                String fieldName = fieldValue.getCategoryMetadataField().getName();
                String values = fieldValue.getValues();
                metadataFilters.putIfAbsent(fieldName, values);
            }
            current = current.getParentCategory();
        }
        return metadataFilters;
    }

    List<String> getAllChildCategoriesIds(Category category) {
        log.debug("Fetching all child category IDs for category ID: {}", category.getCategoryId());
        List<String> categoryIds = new ArrayList<>();
        categoryIds.add(category.getCategoryId());

        Queue<Category> queue = new LinkedList<>();
        queue.add(category);

        while (!queue.isEmpty()) {
            Category current = queue.poll();
            for (Category child : current.getSubCategories()) {
                categoryIds.add(child.getCategoryId());
                queue.add(child);
            }
        }
        return categoryIds;
    }
}
