package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataField;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryMetadataFieldRepository extends JpaRepository<CategoryMetadataField,String> {

    boolean existsByName(String name);
}
