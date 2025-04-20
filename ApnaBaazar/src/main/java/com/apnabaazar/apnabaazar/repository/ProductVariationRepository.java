package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariationRepository extends JpaRepository<ProductVariation, String> {
}
