package com.apnabaazar.apnabaazar.model.dto.category_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MetadataFieldDTO {

    @NotBlank(message = "{metadata.field.name.required}")
    @Size(min = 2, max = 30, message = "{metadata.field.name.size}")
    private String fieldName;
}
