package com.apnabaazar.apnabaazar.specification;

import com.apnabaazar.apnabaazar.model.products.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductSpecification {

    public static Specification<Product> withFilters(Map<String, String> filters, String sellerId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            }
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

    public static Specification<Product> withCustomerFilters(List<String> categoryIds, Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (categoryIds != null && !categoryIds.isEmpty())
                predicates.add(root.get("category").get("categoryId").in(categoryIds));

            predicates.add(cb.isTrue(root.get("active")));

            // Brand filter
            if (filters.containsKey("brand")) {
                predicates.add(cb.equal(root.get("brand"), filters.get("brand")));
            }

            // Product name search
            if (filters.containsKey("name")) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + filters.get("name").toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> withSimilarityFilters(String productId, String categoryId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));

            predicates.add(cb.notEqual(root.get("id"), productId));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}