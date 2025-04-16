package com.apnabaazar.apnabaazar.mapper;

import com.apnabaazar.apnabaazar.model.categories.CategoryMetadataField;
import com.apnabaazar.apnabaazar.model.dto.category_dto.MetadataFieldDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Seller;

public class Mapper {


    public static CustomerResponseDTO fromCustomer(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setId(customer.getId());
        dto.setFullName(buildFullName(customer.getFirstName(), customer.getMiddleName(), customer.getLastName()));
        dto.setEmail(customer.getEmail());
        dto.setActive(customer.isActive());
        return dto;
    }

    public static SellerResponseDTO fromSeller(Seller seller) {
        String fullName = buildFullName(seller.getFirstName(), seller.getMiddleName(), seller.getLastName());

        String companyAddress = seller.getAddresses().stream()
                .findFirst()
                .map(address -> address.getAddressLine() + ", " + address.getCity() + ", " + address.getState() + " - " + address.getZipCode())
                .orElse("N/A");

        return new SellerResponseDTO(
                seller.getId(),
                fullName,
                seller.getEmail(),
                seller.isActive(),
                seller.getCompanyName(),
                seller.getCompanyContact(),
                companyAddress
        );
    }

    private static String buildFullName(String first, String middle, String last) {
        if (middle != null && !middle.isBlank()) {
            return first + " " + middle + " " + last;
        }
        return first + " " + last;
    }


}
