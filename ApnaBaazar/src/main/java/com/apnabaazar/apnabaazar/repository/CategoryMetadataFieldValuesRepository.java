package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataFieldValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryMetadataFieldValuesRepository extends JpaRepository<CategoryMetadataFieldValues, String> {
    List<CategoryMetadataFieldValues> findByCategory(Category category);
}
