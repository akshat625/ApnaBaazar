package com.apnabaazar.apnabaazar.mapper;

import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerProfileDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Customer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    public static List<AddressDTO> toAllAddressDTO(Set<Address> customerAddresses) {
        if (customerAddresses == null || customerAddresses.isEmpty()) {
            return Collections.emptyList();
        }
        return customerAddresses.stream()
                .map(address -> AddressDTO.builder()
                        .addressLine(address.getAddressLine())
                        .city(address.getCity())
                        .country(address.getCountry())
                        .state(address.getState())
                        .zipCode(address.getZipCode())
                        .label(address.getLabel())
                        .build())
                .toList();

    }

    public static Address toAddress(AddressDTO addressDTO) {
        return Address.builder()
                .addressLine(addressDTO.getAddressLine())
                .city(addressDTO.getCity())
                .country(addressDTO.getCountry())
                .state(addressDTO.getState())
                .zipCode(addressDTO.getZipCode())
                .label(addressDTO.getLabel())
                .build();
    }
}
