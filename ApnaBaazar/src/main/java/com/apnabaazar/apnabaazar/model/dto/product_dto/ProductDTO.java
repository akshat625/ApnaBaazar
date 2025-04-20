package com.apnabaazar.apnabaazar.model.dto.product_dto;

import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProductDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String categoryId;
    private String name;
    private String brand;
    private String description;

    private boolean cancellable = false;
    private boolean returnable = false;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean active = false;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    CategoryDTO category;
}
