package com.apnabaazar.apnabaazar.model.products;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_variations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductVariation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String productVariationId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    private String lastModifiedBy;

    @CreatedBy
    private String createdBy;

    @Column(nullable = false)
    private Integer quantityAvailable;

    @Column(nullable = false)
    private Double price;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String,Object> metadata;

    private String primaryImageName;

    @ManyToOne
    private Product product;

    @Column(nullable = false)
    private boolean isActive = true;

}
