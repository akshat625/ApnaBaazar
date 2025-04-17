package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category,String> {

    List<Category> findByParentCategory_CategoryId(String parentCategoryCategoryId);

    Category findByCategoryId(String categoryId);

    Page<Category> findByNameContainingIgnoreCase(String query, Pageable pageable);
}
