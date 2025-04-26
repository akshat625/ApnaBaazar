package com.apnabaazar.apnabaazar.model.dto.product_dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateDTO {


    @Size(max = 255, message = "{product.name.size}")
    private String name;

    @Size(max = 255, message = "{product.brand.size}")
    private String brand;

    @Size(max = 255, message = "{product.description.size}")
    private String description;

    private Boolean cancellable;
    private Boolean returnable;
}

