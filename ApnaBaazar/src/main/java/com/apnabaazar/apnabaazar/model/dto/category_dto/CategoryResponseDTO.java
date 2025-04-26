package com.apnabaazar.apnabaazar.model.dto.category_dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class CategoryResponseDTO {
    private String categoryId;
    private String name;
    private List<CategoryDTO> parentHierarchy;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CategoryDTO> children;
    private List<CategoryMetadataFieldValueDTO> metadataFields;
}

