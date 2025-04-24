package com.apnabaazar.apnabaazar.model.dto.category_dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class CategoryFilterDetailsDTO {
    private String categoryName;
    private Map<String, String> metadataFilters;
    private List<String> brands;
    private Double minPrice;
    private Double maxPrice;
}