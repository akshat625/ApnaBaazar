package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.categories.Category;
import com.apnabaazar.apnabaazar.model.products.Product;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    boolean existsByCategory(Category category);

    boolean existsByNameAndBrandAndCategoryAndSeller(String name, String brand, Category category, User seller);
}
