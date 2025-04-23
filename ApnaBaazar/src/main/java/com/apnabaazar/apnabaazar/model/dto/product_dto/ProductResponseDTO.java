package com.apnabaazar.apnabaazar.model.dto.product_dto;

import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProductResponseDTO {

    ProductDTO product;
    List<ProductVariationResponseDTO> productVariation;

}
