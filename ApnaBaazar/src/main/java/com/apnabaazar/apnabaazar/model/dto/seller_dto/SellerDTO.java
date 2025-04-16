package com.apnabaazar.apnabaazar.model.dto.seller_dto;

import com.apnabaazar.apnabaazar.model.dto.UserDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerDTO extends UserDTO {

    @NotBlank(message = "{gstin.required}")
    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "{gstin.invalid}"
    )
    private String gstin;

    @NotBlank(message = "{company.contact.required}")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "{company.contact.pattern}"
    )
    private String companyContact;

    @NotBlank(message = "{company.name.required}")
    private String companyName;

    @NotBlank(message = "{city.required}")
    private String city;

    @NotBlank(message = "{state.required}")
    private String state;

    @NotBlank(message = "{country.required}")
    private String country;

    @NotBlank(message = "{address.line.required}")
    private String addressLine;

    @NotBlank(message = "{zipcode.required}")
    private String zipCode;
}
