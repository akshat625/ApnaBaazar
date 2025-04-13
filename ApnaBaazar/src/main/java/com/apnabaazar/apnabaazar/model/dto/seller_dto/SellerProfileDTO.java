package com.apnabaazar.apnabaazar.model.dto.seller_dto;

import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerProfileDTO {
    private String id;
    private String firstName;
    private String lastName;
    private boolean isActive;
    private String companyContact;
    private String companyName;
    private String gstin;

    private String profileImageUrl;
    private AddressDTO sellerAddress;
}
