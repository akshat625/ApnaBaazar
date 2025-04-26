package com.apnabaazar.apnabaazar.model.dto.product_dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProductResponseDTO {

    private String sellerId;
    private ProductDTO product;
    private List<ProductVariationResponseDTO> productVariation;

}
