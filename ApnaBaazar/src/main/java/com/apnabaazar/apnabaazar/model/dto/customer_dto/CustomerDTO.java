package com.apnabaazar.apnabaazar.model.dto.customer_dto;

import com.apnabaazar.apnabaazar.model.dto.UserDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDTO extends UserDTO {

    @NotBlank(message = "{contact.required}")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "{contact.pattern}"
    )
    private String contact;
}
