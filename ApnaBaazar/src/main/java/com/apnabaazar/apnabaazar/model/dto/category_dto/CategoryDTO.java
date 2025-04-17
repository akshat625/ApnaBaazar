package com.apnabaazar.apnabaazar.model.dto.category_dto;

import com.apnabaazar.apnabaazar.model.categories.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {

    private String parentId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Category parent;

    private String categoryName;
}
