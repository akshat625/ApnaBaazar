package com.apnabaazar.apnabaazar.model.dto.product_dto;



import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Builder
public class ProductVariationResponseDTO {

    private Map<String,Object> metadata;
    private Integer quantity;
    private Double price;
    private String primaryImageUrl;
    private List<String> secondaryImageUrl;

}
