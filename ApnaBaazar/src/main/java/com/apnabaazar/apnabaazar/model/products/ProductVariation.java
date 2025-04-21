package com.apnabaazar.apnabaazar.model.products;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_variations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String productVariationId;


    private Integer quantityAvailable;

    private Double price;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String,Object> metadata;

    private String primaryImageName;

    @ManyToOne
    private Product product;

//    @OneToMany
//for cart
    private boolean isActive = true;
}

