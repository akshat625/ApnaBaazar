package com.apnabaazar.apnabaazar.model.dto.seller_dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerProfileUpdateDTO {
    private String firstName;
    private String middleName;
    private String lastName;
    private String gstin;
    private String email;
    private String companyName;
    private String companyContact;
}
