package com.apnabaazar.apnabaazar.model.dto.category_dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class CategoryResponseDTO {
    private String categoryId;
    private String name;
    private List<CategoryDTO> parentHierarchy;
    private List<CategoryDTO> children;
    private List<CategoryMetadataFieldValueDTO> metadataFields;
}

