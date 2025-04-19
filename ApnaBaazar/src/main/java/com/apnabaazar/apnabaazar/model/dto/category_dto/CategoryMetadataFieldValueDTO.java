package com.apnabaazar.apnabaazar.model.dto.category_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryMetadataFieldValueDTO {

    private String fieldId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String fieldName;

    private String values;

}