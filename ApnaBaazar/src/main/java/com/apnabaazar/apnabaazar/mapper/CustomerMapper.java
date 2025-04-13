package com.apnabaazar.apnabaazar.mapper;

import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerProfileDTO;
import com.apnabaazar.apnabaazar.model.users.Customer;

public class CustomerMapper {

    public static CustomerProfileDTO toCustomerProfileDTO(Customer customer, String  imageUrl) {
        return CustomerProfileDTO.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .isActive(customer.isActive())
                .contact(customer.getContact())
                .profileImageUrl(imageUrl)
                .build();
    }
}
