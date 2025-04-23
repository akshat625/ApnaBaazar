package com.apnabaazar.apnabaazar.model.dto.product_dto;



import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean active;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> secondaryImageUrl;
}
