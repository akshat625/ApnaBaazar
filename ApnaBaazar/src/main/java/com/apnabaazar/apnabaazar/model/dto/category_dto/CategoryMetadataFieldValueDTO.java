package com.apnabaazar.apnabaazar.model.dto.category_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryMetadataFieldValueDTO {

    @NotBlank(message = "{field.id.required}")
    private String fieldId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String fieldName;

    @NotBlank(message = "{field.values.required}")
    @Size(max = 255, message = "{field.values.size}")
    @Valid
    private String values;

}