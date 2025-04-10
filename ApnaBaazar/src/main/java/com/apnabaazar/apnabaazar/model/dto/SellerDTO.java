package com.apnabaazar.apnabaazar.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerDTO extends UserDTO{

    @NotBlank(message = "GSTIN is required")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GSTIN format"
    )
    private String gstin;

    @NotBlank(message = "Contact number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Contact number must be 10 digits."
    )
    private String companyContact;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Address Line is required")
    private String addressLine;

    @NotBlank(message = "ZipCode is required")
    private String zipCode;

    //set label in service layer
}
