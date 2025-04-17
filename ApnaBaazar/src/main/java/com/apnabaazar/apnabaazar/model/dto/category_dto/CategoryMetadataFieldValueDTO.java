package com.apnabaazar.apnabaazar.model.dto.category_dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoryMetadataFieldValueDTO {
    private String fieldId;
    private String fieldName;
    private List<String> values;
}