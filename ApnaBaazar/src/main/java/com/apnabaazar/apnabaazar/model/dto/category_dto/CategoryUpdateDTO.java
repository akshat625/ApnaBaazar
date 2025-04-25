package com.apnabaazar.apnabaazar.model.dto.category_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryUpdateDTO {

    private String categoryId;

    @NotBlank(message = "{category.not.blank}")
    @Size(max = 100, message = "{category.name.size}")
    private String categoryName;
}
