package com.apnabaazar.apnabaazar.model.dto.product_dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDTO {

    private String categoryId;
    private String name;
    private String brand;

    private boolean cancellable = false;
    private boolean returnable = false;
}
