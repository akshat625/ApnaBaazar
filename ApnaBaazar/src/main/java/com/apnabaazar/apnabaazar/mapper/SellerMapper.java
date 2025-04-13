package com.apnabaazar.apnabaazar.mapper;

import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerProfileDTO;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SellerMapper {

    private final AddressRepository addressRepository;


    public static SellerProfileDTO toSellerProfileDTO(Seller seller, String imageUrl) {
        AddressDTO sellerAddress = seller.getAddresses().stream().findFirst()
                .map(address -> new AddressDTO(
                        address.getAddressLine(),
                        address.getCity(),
                        address.getState(),
                        address.getCountry(),
                        address.getZipCode())).orElse(null);

        return SellerProfileDTO.builder()
                .id(seller.getId())
                .firstName(seller.getFirstName())
                .lastName(seller.getLastName())
                .isActive(seller.isActive())
                .companyContact(seller.getCompanyContact())
                .companyName(seller.getCompanyName())
                .gstin(seller.getGstin())
                .sellerAddress(sellerAddress)
                .profileImageUrl(imageUrl)
                .build();
    }
}
