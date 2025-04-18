package com.apnabaazar.apnabaazar.model.dto.category_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryUpdateDTO {

    private String categoryId;

    private String categoryName;
}
