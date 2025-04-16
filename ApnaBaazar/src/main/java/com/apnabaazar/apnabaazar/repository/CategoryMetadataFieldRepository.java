package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataField;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryMetadataFieldRepository extends JpaRepository<CategoryMetadataField,String> {

    boolean existsByName(String name);

    Page<CategoryMetadataField> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
