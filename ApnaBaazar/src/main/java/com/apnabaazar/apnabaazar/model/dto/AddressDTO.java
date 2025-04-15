package com.apnabaazar.apnabaazar.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    @NotBlank(message = "Address line is required.")
    @Size(max = 255, message = "Address line must not exceed 255 characters.")
    private String addressLine;

    @NotBlank(message = "City is required.")
    @Size(min = 3, max = 50, message = "City name must be between 3 and 50 characters.")
    private String city;

    @NotBlank(message = "State is required.")
    @Size(min = 3, max = 50, message = "State name must be between 3 and 50 characters.")
    private String state;

    @NotBlank(message = "Country is required.")
    @Size(min = 3, max = 50, message = "Country name must be between 3 and 50 characters.")
    private String country;

    @NotBlank(message = "Zip code is required.")
    @Pattern(regexp = "^[0-9]{5,10}$", message = "Zip code must be 5 to 10 digits long and contain only numbers.")
    private String zipCode;

    private String label;

    public AddressDTO(String addressLine, String city, String state, String country, String zipCode) {
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zipCode = zipCode;
    }
}
