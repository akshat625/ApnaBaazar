package com.apnabaazar.apnabaazar.specification;

import com.apnabaazar.apnabaazar.model.products.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductSpecification {

    public static Specification<Product> withFilters(Map<String, String> filters) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String category = filters.get("category");
            String brand = filters.get("brand");
            String name = filters.get("name");
            String active = filters.get("active");

            if (category != null)
                predicates.add(cb.equal(root.get("category").get("categoryId"), category));
            if (brand != null)
                predicates.add(cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%"));
            if (name != null)
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            if (active != null)
                predicates.add(cb.equal(root.get("active"), Boolean.parseBoolean(active)));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


}