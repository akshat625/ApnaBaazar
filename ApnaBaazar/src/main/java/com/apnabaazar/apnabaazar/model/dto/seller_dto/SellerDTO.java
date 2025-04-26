package com.apnabaazar.apnabaazar.model.dto.seller_dto;

import com.apnabaazar.apnabaazar.model.dto.UserDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = 15, message = "{gstin.size}")
    private String gstin;

    @NotBlank(message = "{company.contact.required}")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "{company.contact.pattern}"
    )
    @Size(max = 10, message = "{company.contact.size}")
    private String companyContact;

    @NotBlank(message = "{company.name.required}")
    @Size(max = 255, message = "{company.name.size}")
    private String companyName;

    @NotBlank(message = "{city.required}")
    @Size(max = 100, message = "{city.size}")
    private String city;

    @NotBlank(message = "{state.required}")
    @Size(max = 100, message = "{state.size}")
    private String state;

    @NotBlank(message = "{country.required}")
    @Size(max = 100, message = "{country.size}")
    private String country;

    @NotBlank(message = "{address.line.required}")
    @Size(max = 500, message = "{address.line.size}")
    private String addressLine;

    @NotBlank(message = "{zipcode.required}")
    @Size(max = 10, message = "{zipcode.size}")
    private String zipCode;
}
