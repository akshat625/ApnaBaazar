package com.apnabaazar.apnabaazar.model.dto.category_dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String parentId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String categoryId;

    private String categoryName;
}
