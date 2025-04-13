package com.apnabaazar.apnabaazar.model.dto.seller_dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SellerResponseDTO {
    private String id;
    private String fullName;
    private String email;
    private boolean isActive;
    private String companyName;
    private String companyContact;
    private String companyAddress;
}