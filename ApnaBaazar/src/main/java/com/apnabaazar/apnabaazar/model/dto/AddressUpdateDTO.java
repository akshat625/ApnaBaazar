package com.apnabaazar.apnabaazar.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressUpdateDTO {
    @Size(max = 255, message = "Address line must not exceed 255 characters.")
    private String addressLine;

    @Size(max = 50, message = "City name must not exceed 50 characters.")
    private String city;

    @Size(max = 50, message = "State name must not exceed 50 characters.")
    private String state;

    @Size(max = 50, message = "Country name must not exceed 50 characters.")
    private String country;

    @Pattern(regexp = "^[0-9]{5,10}$", message = "Zip code must be 5 to 10 digits long and contain only numbers.")
    private String zipCode;

    @Size(max = 50, message = "Label name must not exceed 20 characters.")
    private String label;

}
