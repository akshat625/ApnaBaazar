package com.apnabaazar.apnabaazar.model.dto.product_dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ProductVariationUpdateDTO {


    @Size(min = 1, message = "{metadata.min.one}")
    private Map<String,Object> metadata;

    @Min(value = 0, message = "{quantity.min.zero}")
    private Integer quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "{price.min.zero}")
    private Double price;

}
