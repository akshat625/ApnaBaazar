package com.apnabaazar.apnabaazar.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    @NotBlank(message = "{address.line.required}")
    @Size(max = 255, message = "{address.line.size}")
    private String addressLine;

    @NotBlank(message = "{city.required}")
    @Size(min = 3, max = 50, message = "{city.size}")
    private String city;

    @NotBlank(message = "{state.required}")
    @Size(min = 3, max = 50, message = "{state.size}")
    private String state;

    @NotBlank(message = "{country.required}")
    @Size(min = 3, max = 50, message = "{country.size}")
    private String country;

    @NotBlank(message = "{zipcode.required}")
    @Pattern(regexp = "^[0-9]{5,10}$", message = "{zipcode.pattern}")
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
