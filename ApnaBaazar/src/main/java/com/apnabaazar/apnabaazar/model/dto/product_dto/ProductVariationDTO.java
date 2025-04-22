package com.apnabaazar.apnabaazar.model.dto.product_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class ProductVariationDTO {

    @NotBlank(message = "{product.id.not.blank}")
    private String productId;

    @NotNull(message = "{metadata.notnull}")
    @Size(min = 1, message = "{metadata.min.one}")
    private Map<String,Object> metadata;

    @NotNull(message = "{quantity.notnull}")
    @Min(value = 0, message = "{quantity.min.zero}")
    private Integer quantity;

    @NotNull(message = "{price.notnull}")
    @DecimalMin(value = "0.0", inclusive = true, message = "{price.min.zero}")
    private Double price;



    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String productName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String brand;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String description;


}

