package com.apnabaazar.apnabaazar.model.dto;

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

    @Size(max = 255, message = "{address.line.size}")
    private String addressLine;

    @Size(max = 50, message = "{city.size}")
    private String city;

    @Size(max = 50, message = "{state.size}")
    private String state;

    @Size(max = 50, message = "{country.size}")
    private String country;

    @Pattern(regexp = "^[0-9]{5,10}$", message = "{zipcode.pattern}")
    private String zipCode;

    @Size(max = 50, message = "{label.size}")
    private String label;
}
