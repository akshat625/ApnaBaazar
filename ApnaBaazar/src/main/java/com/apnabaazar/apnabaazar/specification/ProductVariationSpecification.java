package com.apnabaazar.apnabaazar.specification;

import com.apnabaazar.apnabaazar.model.products.ProductVariation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductVariationSpecification {

    public static Specification<ProductVariation> withFilters(Map<String, Object> filters, String sellerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();


            if (sellerId != null)
                predicates.add(cb.equal(root.get("product").get("seller").get("id"), sellerId));

            if (filters.containsKey("quantity"))
                predicates.add(cb.equal(root.get("quantityAvailable"), filters.get("quantity")));
            if (filters.containsKey("price"))
                predicates.add(cb.equal(root.get("price"), filters.get("price")));

            if (filters.containsKey("priceMin"))
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), (Double) filters.get("priceMin")));
            if (filters.containsKey("priceMax"))
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), (Double) filters.get("priceMax")));

            if (filters.containsKey("active"))
                predicates.add(cb.equal(root.get("isActive"), filters.get("active")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
