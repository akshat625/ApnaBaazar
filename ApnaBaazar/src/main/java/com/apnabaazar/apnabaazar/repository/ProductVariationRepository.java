package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductVariationRepository extends JpaRepository<ProductVariation, String>, JpaSpecificationExecutor<ProductVariation> {
}
