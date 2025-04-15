package com.apnabaazar.apnabaazar.model.dto.seller_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateDTO {
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "First name must contain only alphabets")
    private String firstName;

    @Size(max = 50, message = "Middle name can be up to 50 characters")
    @Pattern(regexp = "^[A-Za-z]*$", message = "Middle name must contain only alphabets")
    private String middleName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "Last name must contain only alphabets")
    private String lastName;

    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Contact number must be 10 digits."
    )
    private String contact;
}
