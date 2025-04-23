package com.apnabaazar.apnabaazar.model.dto.product_dto;

import com.apnabaazar.apnabaazar.model.dto.category_dto.CategoryDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDTO {

    private String categoryId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String sellerId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String productId;

    @NotBlank(message = "{product.name.not.blank}")
    @Size(max = 255, message = "{product.name.size}")
    private String name;

    @NotBlank(message = "{product.brand.not.blank}")
    @Size(max = 255, message = "{product.brand.size}")
    private String brand;

    @NotBlank(message = "{product.description.not.blank}")
    @Size(max = 1000, message = "{product.description.size}")
    private String description;

    private boolean cancellable = false;
    private boolean returnable = false;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean active = false;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    CategoryDTO category;
}
