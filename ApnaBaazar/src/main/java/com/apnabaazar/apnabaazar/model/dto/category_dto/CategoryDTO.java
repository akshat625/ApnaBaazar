package com.apnabaazar.apnabaazar.model.dto.category_dto;

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
public class CategoryDTO {

    @Size(max = 100, message = "{parent.id.size}")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String parentId;


    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String categoryId;

    @NotBlank(message = "{category.name.required}")
    @Size(max = 100, message = "{category.name.size}")
    private String categoryName;
}
